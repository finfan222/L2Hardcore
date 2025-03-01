package net.sf.l2j.loginserver;

import net.sf.l2j.commons.mmocore.IAcceptFilter;
import net.sf.l2j.commons.mmocore.IClientFactory;
import net.sf.l2j.commons.mmocore.IMMOExecutor;
import net.sf.l2j.commons.mmocore.MMOConnection;
import net.sf.l2j.commons.mmocore.ReceivablePacket;
import net.sf.l2j.loginserver.data.manager.IpBanManager;
import net.sf.l2j.loginserver.network.LoginClient;
import net.sf.l2j.loginserver.network.serverpackets.Init;
import net.sf.l2j.util.IPv4Filter;

import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SelectorHelper implements IMMOExecutor<LoginClient>, IClientFactory<LoginClient>, IAcceptFilter {
    private final ThreadPoolExecutor _generalPacketsThreadPool;

    private final IPv4Filter _ipv4filter;

    public SelectorHelper() {
        _generalPacketsThreadPool = new ThreadPoolExecutor(4, 6, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        _ipv4filter = new IPv4Filter();
    }

    @Override
    public boolean accept(Socket socket) {
        return _ipv4filter.accept(socket) && !IpBanManager.getInstance().isBannedAddress(socket.getInetAddress());
    }

    @Override
    public LoginClient create(MMOConnection<LoginClient> con) {
        LoginClient client = new LoginClient(con);
        client.sendPacket(new Init(client));
        return client;
    }

    @Override
    public void execute(ReceivablePacket<LoginClient> packet) {
        _generalPacketsThreadPool.execute(packet);
    }
}