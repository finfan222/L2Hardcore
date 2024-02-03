package net.sf.l2j.gameserver.network.gameserverpackets;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;

@Slf4j
public class BlowFishKey extends GameServerBasePacket {

    public BlowFishKey(byte[] blowfishKey, RSAPublicKey publicKey) {
        writeC(0x00);
        byte[] encrypted = null;
        try {
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            encrypted = rsaCipher.doFinal(blowfishKey);

            writeD(encrypted.length);
            writeB(encrypted);
        } catch (GeneralSecurityException e) {
            log.error("Error while encrypting blowfish key for transmission.", e);
        }
    }

    @Override
    public byte[] getContent() {
        return getBytes();
    }
}