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
import com.fidd.view.serviceCache.FiddContentServiceCache;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

import javax.annotation.Nullable;

public final class HttpFiddApiServer {
  @Nullable EventLoopGroup bossGroup;
  @Nullable EventLoopGroup workerGroup;
  final int bossGroupThreadCount;
  final int workerGroupThreadCount;
  final FiddContentServiceCache serviceCache;
  final BaseRepositories baseRepositories;

    public HttpFiddApiServer(FiddContentServiceCache serviceCache, BaseRepositories baseRepositories) {
        this(1, 1, serviceCache, baseRepositories);
    }

    public HttpFiddApiServer(int bossGroupThreadCount, int workerGroupThreadCount,
                             FiddContentServiceCache serviceCache, BaseRepositories baseRepositories) {
      this.bossGroupThreadCount = bossGroupThreadCount;
      this.workerGroupThreadCount = workerGroupThreadCount;
      this.serviceCache = serviceCache;
      this.baseRepositories = baseRepositories;
    }

  public Channel startServer(boolean ssl, int port) throws Exception {
    // TODO: why ssl parameter is ignored?
    // Configure SSL.
    final SslContext sslCtx = ServerUtil.buildSslContext();

    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup)
      .channel(NioServerSocketChannel.class)
      .handler(new LoggingHandler(LogLevel.DEBUG))
      .childHandler(new HttpFiddApiServerInitializer(sslCtx, null, serviceCache, baseRepositories));

     return b.bind(port).sync().channel();
  }

  public void stopServer() throws Exception {
    if (bossGroup != null) { bossGroup.shutdownGracefully(); }
    if (workerGroup != null) { workerGroup.shutdownGracefully(); }
  }

  public static HttpFiddApiServer runServer(FiddContentServiceCache serviceCache, BaseRepositories baseRepositories, int port) throws Exception {
    HttpFiddApiServer fiddApiServer = new HttpFiddApiServer(serviceCache, baseRepositories);
    fiddApiServer.startServer(false, port);
    return fiddApiServer;
  }
}