/**
 * Tencent is pleased to support the open source community by making Tars available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.qq.tars.server.core;

import com.qq.tars.common.support.Endpoint;
import com.qq.tars.protocol.annotation.ServantCodec;
import com.qq.tars.rpc.exc.TarsException;
import com.qq.tars.rpc.protocol.Codec;
import com.qq.tars.rpc.protocol.tars.TarsServerCodec;
import com.qq.tars.server.config.ConfigurationManager;
import com.qq.tars.server.config.ServantAdapterConfig;
import com.qq.tars.server.config.ServerConfig;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.Executor;

public class ServantAdapter implements Adapter {


    private final ServantAdapterConfig servantAdapterConfig;

    private ServantHomeSkeleton skeleton;

    public ServantAdapter(ServantAdapterConfig servantAdapterConfig) {
        this.servantAdapterConfig = servantAdapterConfig;
    }

    public void bind() throws IOException {
        ServerConfig serverCfg = ConfigurationManager.getInstance().getServerConfig();
        boolean keepAlive = true;
        Codec codec = new TarsServerCodec(serverCfg.getCharsetName());
        Processor processor = new TarsServantProcessor();
        Executor threadPool = ServantThreadPoolManager.get(servantAdapterConfig);

        Endpoint endpoint = this.servantAdapterConfig.getEndpoint();
        if (endpoint.type().equals("tcp")) {

            System.out.println("[SERVER] server starting at " + endpoint + "...");
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(endpoint.host(), endpoint.port()), 1024);
            serverChannel.configureBlocking(false);


            System.out.println("[SERVER] server started at " + endpoint + "...");

        } else if (endpoint.type().equals("udp")) {

            System.out.println("[SERVER] server starting at " + endpoint + "...");
            DatagramChannel serverChannel = DatagramChannel.open();
            DatagramSocket socket = serverChannel.socket();
            socket.bind(new InetSocketAddress(endpoint.host(), endpoint.port()));
            serverChannel.configureBlocking(false);

            System.out.println("[SERVER] servant started at " + endpoint + "...");
        }
    }

    public void bind(AppService appService) throws IOException {
        this.skeleton = (ServantHomeSkeleton) appService;
        ServerConfig serverCfg = ConfigurationManager.getInstance().getServerConfig();

        boolean keepAlive = true;
        Codec codec = createCodec(serverCfg);
        Processor processor = createProcessor(serverCfg);
        Executor threadPool = ServantThreadPoolManager.get(servantAdapterConfig);

        Endpoint endpoint = this.servantAdapterConfig.getEndpoint();
        if (endpoint.type().equals("tcp")) {
            System.out.println("[SERVER] server starting at " + endpoint + "...");
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(endpoint.host(), endpoint.port()), 1024);
            serverChannel.configureBlocking(false);

            System.out.println("[SERVER] server started at " + endpoint + "...");

        } else if (endpoint.type().equals("udp")) {
            System.out.println("[SERVER] server starting at " + endpoint + "...");
            DatagramChannel serverChannel = DatagramChannel.open();
            DatagramSocket socket = serverChannel.socket();
            socket.bind(new InetSocketAddress(endpoint.host(), endpoint.port()));
            serverChannel.configureBlocking(false);
            System.out.println("[SERVER] servant started at " + endpoint + "...");
        }
    }

    public ServantAdapterConfig getServantAdapterConfig() {
        return servantAdapterConfig;
    }

    public ServantHomeSkeleton getSkeleton() {
        return skeleton;
    }

    private Processor createProcessor(ServerConfig serverCfg) throws TarsException {
        Processor processor = null;
        Class<? extends Processor> processorClass = skeleton.getProcessorClass();
        if (processorClass == null) {
            return new TarsServantProcessor();
        }

        if (processorClass != null) {
            Constructor<? extends Processor> constructor;
            try {
                constructor = processorClass.getConstructor(new Class[]{ServantAdapter.class});
                processor = constructor.newInstance(this);
            } catch (Exception e) {
                throw new TarsException("error occurred on create codec, codec=" + processorClass.getName());
            }
        }
        return processor;
    }

    private Codec createCodec(ServerConfig serverCfg) throws TarsException {
        Codec codec = null;
        Class<? extends Codec> codecClass = skeleton.getCodecClass();
        if (codecClass == null) {
            ServantCodec servantCodec = skeleton.getApiClass().getAnnotation(ServantCodec.class);
            if (servantCodec != null) {
                codecClass = servantCodec.codec();
            }
        }

        if (codecClass == null) {
            codecClass = TarsServerCodec.class;
        }

        if (codecClass != null) {
            Constructor<? extends Codec> constructor;
            try {
                constructor = codecClass.getConstructor(new Class[]{String.class});
                codec = constructor.newInstance(serverCfg.getCharsetName());
            } catch (Exception e) {
                throw new TarsException("error occurred on create codec, codec=" + codecClass.getName());
            }
        }
        return codec;
    }

    public void stop() {
    }
}
