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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;

import javax.annotation.Nullable;

public class HttpFiddApiServerInitializer extends ChannelInitializer<SocketChannel> {

  @Nullable
  private final SslContext sslCtx;
  @Nullable private final BasicCredentials credentials;
  private final FiddContentServiceCache serviceCache;
  private final BaseRepositories baseRepositories;

  public HttpFiddApiServerInitializer(@Nullable SslContext sslCtx, @Nullable BasicCredentials credentials,
                                      FiddContentServiceCache serviceCache, BaseRepositories baseRepositories) {
    this.sslCtx = sslCtx;
    this.credentials = credentials;

    this.serviceCache = serviceCache;
    this.baseRepositories = baseRepositories;
  }

  @Override
  public void initChannel(SocketChannel ch) {
    ChannelPipeline pipeline = ch.pipeline();
    if (sslCtx != null) {
      pipeline.addLast(sslCtx.newHandler(ch.alloc()));
    }
    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new HttpObjectAggregator(65536));
    pipeline.addLast(new ChunkedWriteHandler());
    if (credentials != null) {
      pipeline.addLast(new HttpBasicAuthHandler(credentials.username(), credentials.password()));
    }
    pipeline.addLast(new HttpFiddApiServerHandler(serviceCache, baseRepositories));
  }
}