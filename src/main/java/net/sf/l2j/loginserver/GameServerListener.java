package net.sf.l2j.loginserver;

import net.sf.l2j.Config;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServerListener extends FloodProtectedListener {
    private static List<GameServerThread> _gameServers = new ArrayList<>();

    public GameServerListener() throws IOException {
        super(Config.GAMESERVER_LOGIN_HOSTNAME, Config.GAMESERVER_LOGIN_PORT);
    }

    @Override
    public void addClient(Socket s) {
        _gameServers.add(new GameServerThread(s));
    }

    public void removeGameServer(GameServerThread gst) {
        _gameServers.remove(gst);
    }
}