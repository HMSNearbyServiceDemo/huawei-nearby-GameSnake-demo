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

package com.huawei.nearby.game.snake;

import android.content.Context;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Align;
import com.esotericsoftware.minlog.Log;
import com.huawei.hms.nearby.Nearby;
import com.huawei.hms.nearby.discovery.DiscoveryEngine;
import com.huawei.hms.nearby.transfer.TransferEngine;
import com.kotcrab.vis.ui.VisUI;
import com.huawei.nearby.game.snake.agents.Client;
import com.huawei.nearby.game.snake.agents.IAgent;
import com.huawei.nearby.game.snake.agents.Server;
import com.huawei.nearby.game.snake.helpers.Constants;
import com.huawei.nearby.game.snake.states.ErrorState;
import com.huawei.nearby.game.snake.states.GameState;
import com.huawei.nearby.game.snake.states.TitleScreenState;

import java.util.Stack;

public class App extends Game {
    protected Stack<GameState> stateStack;

    protected IAgent agent;

    private TransferEngine mTransferEngine;

    private DiscoveryEngine mDiscoveryEngine;

    private Context mContext;

    public App(Context context) {
        this.mContext = context;
        this.mTransferEngine = Nearby.getTransferEngine(context);
        this.mDiscoveryEngine = Nearby.getDiscoveryEngine(context);
    }

    public IAgent getAgent() {
        return agent;
    }

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_NONE);
        Log.set(Log.LEVEL_NONE);
        VisUI.load(VisUI.SkinScale.X2);
        VisUI.setDefaultTitleAlign(Align.center);

        stateStack = new Stack<GameState>();
        pushState(new TitleScreenState(this));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        destroyAgent();
        while (!stateStack.empty()) {
            stateStack.pop().dispose();
        }
        VisUI.dispose();
    }

    public void pushState(GameState gameState) {
        if (!stateStack.isEmpty()) {
            stateStack.peek().pause();
        }
        stateStack.push(gameState);
        setScreen(gameState);
    }

    public void popState() {
        stateStack.pop().dispose();
        setScreen(stateStack.peek());
        stateStack.peek().resume();
    }

    public void setState(GameState gameState) {
        stateStack.pop().dispose();
        pushState(gameState);
    }

    public GameState getCurState() {
        return stateStack.peek();
    }

    public void gotoTitleScreen() {
        while (stateStack.size() > 1) {
            popState();
        }
        destroyAgent();
    }

    public void gotoErrorScreen(String errorMessage) {
        while (stateStack.size() > 1) {
            popState();
        }
        destroyAgent();
        pushState(new ErrorState(this, errorMessage));
    }

    public void initAgent(boolean isServer) {
        if (isServer) {
            agent = new Server(mTransferEngine, mDiscoveryEngine, this);
        } else {
            agent = new Client(mTransferEngine, mDiscoveryEngine, this);
        }
    }

    public void destroyAgent() {
        if (agent != null) {
            agent.destroy();
        }
    }
}
