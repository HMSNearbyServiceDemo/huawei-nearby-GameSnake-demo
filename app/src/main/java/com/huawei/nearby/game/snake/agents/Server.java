/*
Copyright 2018 Tianyi Zhang

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


    2020.3.15 - Replaced Kryonet by HMS Nearby Service.
    2020.3.15 - Add Chinese version.
    2020.3.15 - Add Game speed selection.
                Huawei Technologies Co.,Ltd. <wangmingqi@huawei.com>
 */

package com.huawei.nearby.game.snake.agents;

import java.util.HashMap;
import java.util.Map;
import com.huawei.hms.nearby.StatusCode;
import com.huawei.hms.nearby.discovery.BroadcastOption;
import com.huawei.hms.nearby.discovery.ConnectCallback;
import com.huawei.hms.nearby.discovery.ConnectInfo;
import com.huawei.hms.nearby.discovery.ConnectResult;
import com.huawei.hms.nearby.discovery.DiscoveryEngine;
import com.huawei.hms.nearby.discovery.Policy;
import com.huawei.hms.nearby.transfer.Data;
import com.huawei.hms.nearby.transfer.DataCallback;
import com.huawei.hms.nearby.transfer.TransferEngine;
import com.huawei.hms.nearby.transfer.TransferStateUpdate;
import com.huawei.nearby.game.snake.App;
import com.huawei.nearby.game.snake.elements.ServerSnapshot;
import com.huawei.nearby.game.snake.states.GameState;
import com.huawei.nearby.game.snake.states.server.BroadcastState;
import com.huawei.nearby.game.snake.states.server.SVMainGameState;
import android.util.Log;

public class Server extends IAgent {
    private static final String TAG = "NearbyGameSnake";
    private TransferEngine mTransferEngine;
    private DiscoveryEngine mDiscoveryEngine;
    private String myNameStr = "NearbySnakeServer";
    private String myServiceId = "NearbySnakeServiceId";
    private Map<String, String> remoteIdNameMap = new HashMap<String, String>();
    private App _app;

    public Server(TransferEngine mTransferEngine, DiscoveryEngine mDiscoveryEngine, App app) {
        this.mTransferEngine = mTransferEngine;
        this.mDiscoveryEngine = mDiscoveryEngine;
        _app = app;
    }

    @Override
    public void broadcast()  {
        Log.d(TAG, "Start Nearby broadcast.");
        BroadcastOption.Builder advBuilder = new BroadcastOption.Builder();
        advBuilder.setPolicy(Policy.POLICY_STAR);
        mDiscoveryEngine.startBroadcasting(myNameStr, myServiceId, mConnCb, advBuilder.build());
    }

    @Override
    public void lookForServer() {

    }

    @Override
    public void send(byte[] packet) {
        Data data = Data.fromBytes(packet);
        for (String endpointId : remoteIdNameMap.keySet()) {
            Log.d(TAG, "Server sent data to id:" + endpointId + " Data id: " + data.getId());
            mTransferEngine.sendData(endpointId, data);
        }

    }

    @Override
    public void destroy() {
        mDiscoveryEngine.stopBroadcasting();
        mDiscoveryEngine.disconnectAll();
    }

    private ConnectCallback mConnCb = new ConnectCallback() {
        @Override
        public void onEstablish(String endpointId, ConnectInfo connectionInfo) {
            mDiscoveryEngine.acceptConnect(endpointId, mDataCb);
            remoteIdNameMap.put(endpointId, connectionInfo.getEndpointName());
            Log.d(TAG,
                "Nearby Server accept connection from: Id" + endpointId + "Name" + connectionInfo.getEndpointName());
        }

        @Override
        public void onResult(String endpointId, ConnectResult result) {
            switch (result.getStatus().getStatusCode()) {
                case StatusCode.STATUS_SUCCESS:
                    /* The connection was established successfully, we can exchange data. */
                    Log.d(TAG, "Nearby connection was established successfully. Remote ID:" + endpointId);
                    mDiscoveryEngine.stopBroadcasting();
                    clientConnected(endpointId);
                    break;
                default:
                    /* other status code. */
            }
        }

        @Override
        public void onDisconnected(String endpointId) {
            Log.d(TAG, "Nearby Client Disconnected from:" + endpointId);
            disconnectedFromServer(endpointId);
        };
    };

    private DataCallback mDataCb = new DataCallback() {
        @Override
        public void onReceived(String endpointId, Data data) {
            Log.d(TAG, "Server received data.from" + endpointId + " DataID: " + data.getId());
            processClientData(endpointId, data);
        }

        @Override
        public void onTransferUpdate(String string, TransferStateUpdate update) {
        }
    };

    /* Client Connected. Change UI */
    private void clientConnected(String endpointId) {
        GameState state = _app.getCurState();
        if (state instanceof BroadcastState) {
            BroadcastState broadcastState = (BroadcastState) state;
            synchronized (broadcastState.connectionIdsLock) {
                String remoteEndpointName = remoteIdNameMap.get(endpointId);
                broadcastState.connectionIds.add(Integer.valueOf(remoteEndpointName));
                if (broadcastState.connectionIds.size() == 0) {
                    broadcastState.btnStart.setDisabled(true);
                    broadcastState.lblPlayerCount.setText("0");
                } else {

                    broadcastState.lblPlayerCount.setText(
                            String.format(broadcastState.PLAYERS_CONNECTED_FORMAT, broadcastState.connectionIds.size()));
                    for (int i = 5; i > 0; i--) {
                        String str = "1 other Snake has joined the game.\nYou can start game after " + i + " seconds.";
                        broadcastState.btnStart.setText(String.format("\n          %d         \n", i));
                        Log.d(TAG, str);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    broadcastState.btnStart.setText("\n        GO!        \n");
                    broadcastState.btnStart.setDisabled(false);
                }
            }
        }
    }
    // when client disconnected from server. Change UI.
    private void disconnectedFromServer(String endpointId) {
        GameState state = _app.getCurState();
        if (state instanceof BroadcastState) {
            Log.d(TAG, "disconnect at broadcast state");
            BroadcastState broadcastState = (BroadcastState) state;
            synchronized (broadcastState.connectionIdsLock) {
                String remoteEndpointName = remoteIdNameMap.get(endpointId);
                broadcastState.connectionIds.remove(Integer.valueOf(remoteEndpointName));
                if (broadcastState.connectionIds.size() == 0) {
                    Log.d(TAG, "no snakes left after disconnect");
                    broadcastState.btnStart.setDisabled(true);
                    broadcastState.lblPlayerCount.setText("0");
                } else {
                    Log.d(TAG, broadcastState.connectionIds.size() + " snakes left after disconnect");
                    broadcastState.btnStart.setDisabled(false);
                    broadcastState.lblPlayerCount.setText(
                            String.format(broadcastState.PLAYERS_CONNECTED_FORMAT, broadcastState.connectionIds.size()));
                }
            }
        } else if (state instanceof SVMainGameState) {
            SVMainGameState svMainGameState = (SVMainGameState) state;
            svMainGameState.serverSnapshot.onClientDisconnected(Integer.valueOf(endpointId));
        }
    }
    //When Server receive data from Client
    private void processClientData(String endpointId, Data data) {
        GameState state = _app.getCurState();
        if (state instanceof SVMainGameState) {
            SVMainGameState mainGameState = (SVMainGameState) state;

            /* process client game data */
            byte[] bytes = data.asBytes();
            ServerSnapshot serverSnapshot = ((SVMainGameState) state).getServerSnapshot();
            String remoteEndpointName = remoteIdNameMap.get(endpointId);
            serverSnapshot.onClientMessage(Integer.valueOf(remoteEndpointName), Server.parseClientMessage(bytes));
        }
    }
}
