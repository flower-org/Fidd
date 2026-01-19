package com.fidd.view.http;
/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import com.fidd.base.BaseRepositories;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import com.fidd.service.FiddContentService;
import com.fidd.service.LogicalFileInfo;
import com.fidd.view.serviceCache.FiddContentServiceCache;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT_RANGES;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_RANGE;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * A simple handler that serves incoming HTTP requests to send their respective
 * HTTP responses.  It also implements {@code 'If-Modified-Since'} header to
 * take advantage of browser cache, as described in
 * <a href="https://tools.ietf.org/html/rfc2616#section-14.25">RFC 2616</a>.
 *
 * <h3>How Browser Caching Works</h3>
 *
 * Web browser caching works with HTTP headers as illustrated by the following
 * sample:
 * <ol>
 * <li>Request #1 returns the content of {@code /file1.txt}.</li>
 * <li>Contents of {@code /file1.txt} is cached by the browser.</li>
 * <li>Request #2 for {@code /file1.txt} does not return the contents of the
 *     file again. Rather, a 304 Not Modified is returned. This tells the
 *     browser to use the contents stored in its cache.</li>
 * <li>The server knows the file has not been modified because the
 *     {@code If-Modified-Since} date is the same as the file's last
 *     modified date.</li>
 * </ol>
 *
 * <pre>
 * Request #1 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 *
 * Response #1 Headers
 * ===================
 * HTTP/1.1 200 OK
 * Date:               Tue, 01 Mar 2011 22:44:26 GMT
 * Last-Modified:      Wed, 30 Jun 2010 21:36:48 GMT
 * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
 * Cache-Control:      private, max-age=31536000
 *
 * Request #2 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 * If-Modified-Since:  Wed, 30 Jun 2010 21:36:48 GMT
 *
 * Response #2 Headers
 * ===================
 * HTTP/1.1 304 Not Modified
 * Date:               Tue, 01 Mar 2011 22:44:28 GMT
 *
 * </pre>
 */
public class HttpFiddApiServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  final static Logger LOGGER = LoggerFactory.getLogger(HttpFiddApiServerHandler.class);

  public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
  public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
  public static final int HTTP_CACHE_SECONDS = 60;

  public static final Pattern URI_PATTERN = Pattern.compile("^/([^/]+)/([^/]+)/(.*)$");
  private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");


  public static final AtomicInteger TRANSFERS = new AtomicInteger();

  @Nullable
  private FullHttpRequest request;

  final FiddContentServiceCache serviceCache;
  final BaseRepositories baseRepositories;

  public HttpFiddApiServerHandler(FiddContentServiceCache serviceCache, BaseRepositories baseRepositories) {
      this.serviceCache = serviceCache;
      this.baseRepositories = baseRepositories;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
    this.request = request;
    if (!request.decoderResult().isSuccess()) {
      sendError(ctx, BAD_REQUEST);
      return;
    }

    if (!GET.equals(request.method())) {
      sendError(ctx, METHOD_NOT_ALLOWED);
      return;
    }

    // TODO: maybe just set to false?
    final boolean keepAlive = HttpUtil.isKeepAlive(request);
    final String uri = request.uri();
    final String path = sanitizeUri(uri);
    if (path == null) {
      sendError(ctx, BAD_REQUEST);
      return;
    }

    String fiddName;
    long messageNumber;
    String filePath;

    Matcher m = URI_PATTERN.matcher(uri);
    if (m.matches()) {
      fiddName = m.group(1);
      messageNumber = Long.parseLong(m.group(2));
      filePath = m.group(3);
    } else {
      sendError(ctx, BAD_REQUEST);
      return;
    }

    FiddContentService fiddService = serviceCache.getService(fiddName);
    if (fiddService == null) {
      sendError(ctx, NOT_FOUND);
      return;
    }

    LogicalFileInfo logicalFileInfo = null;
    List<LogicalFileInfo> logicalFileInfos = fiddService.getLogicalFileInfos(messageNumber);
    if (logicalFileInfos == null) {
      sendError(ctx, NOT_FOUND);
      return;
    }
    for (LogicalFileInfo candidate : logicalFileInfos) {
        if (candidate.metadata().filePath().equals(filePath)) {
          logicalFileInfo = candidate;
          break;
        }
    }
    if (logicalFileInfo == null) {
      sendError(ctx, NOT_FOUND);
      return;
    }

    // Cache Validation
    long fileUpdateTime = logicalFileInfo.metadata().updatedAt() == null
            ? System.currentTimeMillis()
            : logicalFileInfo.metadata().updatedAt();
    String ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
    if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
      SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
      Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

      // Only compare up to the second because the datetime format we send to the client
      // does not have milliseconds
      long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
      long fileLastModifiedSeconds = fileUpdateTime / 1000;
      if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
        sendNotModified(ctx);
        return;
      }
    }

    long fileLengthEncrypted = logicalFileInfo.section().sectionLength() - logicalFileInfo.fileOffset();
    EncryptionAlgorithm encryptionAlgorithm = baseRepositories.encryptionAlgorithmRepo().get(logicalFileInfo.section().encryptionAlgorithm());
    long fileLength = checkNotNull(encryptionAlgorithm).plaintextLengthToCiphertextLength(fileLengthEncrypted);

    HttpResponse response;
    InputStream responseFileStream;
    ChannelFuture sendFileFuture;
    ChannelFuture lastContentFuture;
    boolean headersGotRange = request.headers() != null && request.headers().contains(HttpHeaderNames.RANGE);
    if (!headersGotRange) {
      response = new DefaultHttpResponse(HTTP_1_1, OK);
      HttpUtil.setContentLength(response, fileLength);

      responseFileStream = fiddService.readLogicalFile(messageNumber, logicalFileInfo);
      if (responseFileStream == null) {
        sendError(ctx, NOT_FOUND);
        return;
      }

      int transfers = TRANSFERS.incrementAndGet();
      LOGGER.info("{} File: {}. act={} Chunked transfer started.", ctx.channel().id(), filePath, transfers);
    } else {
      //TODO: the standard also describes multiple ranges in the request, like "Range: 0-99,200-299"
      // ideally we want to support that, but practically speaking video streaming (ffplay, VLC, browsers)
      // works just fine without it, utilizing only singe range requests.
      if (!(encryptionAlgorithm instanceof RandomAccessEncryptionAlgorithm raEncryptionAlgorithm)) {
        sendError(ctx, REQUESTED_RANGE_NOT_SATISFIABLE);
        return;
      }

      String rangeStr = request.headers().get(HttpHeaderNames.RANGE);
      rangeStr = rangeStr.replace("bytes=", "");
      String[] rangeParts = rangeStr.split("-");
      long startPos = 0;
      long endPos = fileLength - 1;
      if (!StringUtils.isBlank(rangeParts[0])) {
        startPos = Long.parseLong(rangeParts[0]);
      }
      if (rangeParts.length > 1 && !StringUtils.isBlank(rangeParts[1])) {
        endPos = Long.parseLong(rangeParts[1]);
      }

      if (startPos >= fileLength || endPos >= fileLength) {
        FullHttpResponse rangeErrorResponse = new DefaultFullHttpResponse(HTTP_1_1, REQUESTED_RANGE_NOT_SATISFIABLE);
        rangeErrorResponse.headers().set(CONTENT_RANGE, "bytes */" + fileLength);

        sendAndCleanupConnection(ctx, rangeErrorResponse);
        return;
      }

      long contentLength = endPos - startPos + 1;
      response = new DefaultHttpResponse(HTTP_1_1, PARTIAL_CONTENT);
      HttpUtil.setContentLength(response, contentLength);
      response.headers().set(CONTENT_RANGE, "bytes " + startPos + "-" + endPos + "/" + fileLength);
      response.headers().set(ACCEPT_RANGES, "bytes");

      responseFileStream = fiddService.readLogicalFileChunk(messageNumber, logicalFileInfo, startPos, contentLength);
      if (responseFileStream == null) {
        sendError(ctx, NOT_FOUND);
        return;
      }

      int transfers = TRANSFERS.incrementAndGet();
      LOGGER.info("{} File: {}. act={} st={} end={} len={}  Chunked rng transfer started.", ctx.channel().id(), filePath, transfers, startPos, endPos, contentLength);
    }

    setContentTypeHeader(response, path);
    setDateAndCacheHeaders(response, new Date(fileUpdateTime));

    if (!keepAlive) {
      response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    } else if (request.protocolVersion().equals(HTTP_1_0)) {
      response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }

    // Write the initial line and the header.
    ctx.write(response);

    // Always chunked now, because InputStream cannot be zero-copy
    sendFileFuture = ctx.writeAndFlush(
            new HttpChunkedInput(new ChunkedStream(responseFileStream, 8192)),
            ctx.newProgressivePromise()
    );

    // HttpChunkedInput writes LastHttpContent automatically
    lastContentFuture = sendFileFuture;

    sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
      @Override
      public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
        if (total < 0) { // total unknown
          LOGGER.debug("{} File: {}. Transfer progress: {}", future.channel().id(), filePath, progress);
        } else {
          LOGGER.debug("{} File: {}. Transfer progress: {} / {}", future.channel().id(), filePath, progress, total);
        }
      }

      @Override
      public void operationComplete(ChannelProgressiveFuture future) {
        int transfers = TRANSFERS.decrementAndGet();
        LOGGER.info("{} File: {}. act={} Transfer complete.", future.channel().id(), filePath, transfers);
      }
    });

    // Decide whether to close the connection or not.
    if (!keepAlive) {
      // Close the connection when the whole content is written out.
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOGGER.error("HttpStaticFileServerHandler exceptionCaught", cause);
    if (ctx.channel().isActive()) {
      sendError(ctx, INTERNAL_SERVER_ERROR);
    }
  }

  @Nullable
  private static String sanitizeUri(String uri) {
    // Decode the path.
      uri = URLDecoder.decode(uri, StandardCharsets.UTF_8);

      if (uri.isEmpty() || uri.charAt(0) != '/') {
      return null;
    }

    // Simplistic dumb security check.
    // You will have to do something serious in the production environment.
    if (uri.contains(File.separator + '.') ||
      uri.contains('.' + File.separator) ||
      uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.' ||
      INSECURE_URI.matcher(uri).matches()) {
      return null;
    }

    return uri;
  }

  private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

    sendAndCleanupConnection(ctx, response);
  }

  /**
   * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
   *
   * @param ctx
   *            Context
   */
  private void sendNotModified(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED, Unpooled.EMPTY_BUFFER);
    setDateHeader(response);

    sendAndCleanupConnection(ctx, response);
  }

  /**
   * If Keep-Alive is disabled, attaches "Connection: close" header to the response
   * and closes the connection after the response being sent.
   */
  private void sendAndCleanupConnection(ChannelHandlerContext ctx, FullHttpResponse response) {
    final FullHttpRequest request = this.request;
    final boolean keepAlive = HttpUtil.isKeepAlive(request);
    HttpUtil.setContentLength(response, response.content().readableBytes());
    if (!keepAlive) {
      // We're going to close the connection as soon as the response is sent,
      // so we should also make it clear for the client.
      response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    } else if (checkNotNull(request).protocolVersion().equals(HTTP_1_0)) {
      response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }

    ChannelFuture flushPromise = ctx.writeAndFlush(response);

    if (!keepAlive) {
      // Close the connection as soon as the response is sent.
      flushPromise.addListener(ChannelFutureListener.CLOSE);
    }
  }

  /** Sets the Date header for the HTTP response */
  private static void setDateHeader(FullHttpResponse response) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

    Calendar time = new GregorianCalendar();
    response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));
  }

  /** Sets the Date and Cache headers for the HTTP Response */
  private static void setDateAndCacheHeaders(HttpResponse response, Date lastModified) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

    // Date header
    Calendar time = new GregorianCalendar();
    response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

    // Add cache headers
    time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
    response.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
    response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
    response.headers().set(
      HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(lastModified));
  }

  /**
   * Sets the content type header for the HTTP Response
   *
   * @param response
   *            HTTP response
   * @param filePath
   *            file to extract content type
   */
  private static void setContentTypeHeader(HttpResponse response, String filePath) {
    int dot_pos = filePath.lastIndexOf(".");
    if (dot_pos >= 0) {
      String fileExt = filePath.substring(dot_pos + 1);
      if (fileExt.equals("mp4")) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "video/mp4");
        return;
      } else if (fileExt.equals("log")) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        return;
      } else if (fileExt.equals("svg")) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/svg+xml");
        return;
      }
    }

    MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType(filePath));
  }
}