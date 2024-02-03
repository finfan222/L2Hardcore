package net.sf.l2j.loginserver.network.gameserverpackets;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.loginserver.network.clientpackets.ClientBasePacket;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;

@Slf4j
public class BlowFishKey extends ClientBasePacket {

    byte[] _key;

    public BlowFishKey(byte[] decrypt, RSAPrivateKey privateKey) {
        super(decrypt);

        int size = readD();
        byte[] tempKey = readB(size);

        try {
            final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);

            final byte[] tempDecryptKey = rsaCipher.doFinal(tempKey);

            // there are nulls before the key we must remove them
            int i = 0;
            int len = tempDecryptKey.length;
            for (; i < len; i++) {
                if (tempDecryptKey[i] != 0) {
                    break;
                }
            }
            _key = new byte[len - i];
            System.arraycopy(tempDecryptKey, i, _key, 0, len - i);
        } catch (GeneralSecurityException e) {
            log.error("Couldn't decrypt blowfish key (RSA)", e);
        }
    }

    public byte[] getKey() {
        return _key;
    }
}