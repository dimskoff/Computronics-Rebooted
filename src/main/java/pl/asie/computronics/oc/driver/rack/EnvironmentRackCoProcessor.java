package pl.asie.computronics.oc.driver.rack;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ComponentConnectableRackMountableEnvironment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.CRC32;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EnvironmentRackCoProcessor extends ComponentConnectableRackMountableEnvironment {
    private final EnvironmentHost host;
    private final SecureRandom random = new SecureRandom();

    public EnvironmentRackCoProcessor(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("co_processor", Visibility.Network)
                .create());
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Processor,
                DeviceInfo.DeviceAttribute.Description, "Rack Cryptographic Co-Processor",
                DeviceInfo.DeviceAttribute.Vendor, "Computronics",
                DeviceInfo.DeviceAttribute.Product, "CryptoASIC-v2",
                DeviceInfo.DeviceAttribute.Clock, "250MHz"
        );
    }

    @Callback(direct = true, doc = "function(data:string):string -- Computes MD5 hash.")
    public Object[] md5(Context context, Arguments args) {
        try {
            byte[] bytes = args.checkByteArray(0);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            return new Object[]{HexFormat.of().formatHex(digest)};
        } catch (Exception e) {
            return new Object[]{null, e.getMessage()};
        }
    }

    @Callback(direct = true, doc = "function(data:string):string -- Computes SHA-256 hash.")
    public Object[] sha256(Context context, Arguments args) {
        try {
            byte[] bytes = args.checkByteArray(0);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return new Object[]{HexFormat.of().formatHex(digest)};
        } catch (Exception e) {
            return new Object[]{null, e.getMessage()};
        }
    }

    @Callback(direct = true, doc = "function(data:string):number -- Computes CRC32 checksum.")
    public Object[] crc32(Context context, Arguments args) {
        byte[] bytes = args.checkByteArray(0);
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return new Object[]{crc.getValue()};
    }

    @Callback(doc = "function(data:string, key:string):string -- Encrypts data with AES-128 (key padded to 16 bytes), returns Base64.")
    public Object[] encrypt(Context context, Arguments args) {
        try {
            byte[] data = args.checkByteArray(0);
            byte[] keyBytes = args.checkByteArray(1);
            byte[] keyPadded = new byte[16];
            System.arraycopy(keyBytes, 0, keyPadded, 0, Math.min(keyBytes.length, 16));

            SecretKeySpec keySpec = new SecretKeySpec(keyPadded, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data);
            return new Object[]{Base64.getEncoder().encodeToString(encrypted)};
        } catch (Exception e) {
            return new Object[]{null, e.getMessage()};
        }
    }

    @Callback(doc = "function(base64Data:string, key:string):string -- Decrypts AES-128 Base64 data.")
    public Object[] decrypt(Context context, Arguments args) {
        try {
            String b64 = args.checkString(0);
            byte[] data = Base64.getDecoder().decode(b64);
            byte[] keyBytes = args.checkByteArray(1);
            byte[] keyPadded = new byte[16];
            System.arraycopy(keyBytes, 0, keyPadded, 0, Math.min(keyBytes.length, 16));

            SecretKeySpec keySpec = new SecretKeySpec(keyPadded, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(data);
            return new Object[]{new String(decrypted, StandardCharsets.UTF_8)};
        } catch (Exception e) {
            return new Object[]{null, e.getMessage()};
        }
    }

    @Callback(direct = true, doc = "function(bytes:number):string -- Generates secure random hex string.")
    public Object[] generateKey(Context context, Arguments args) {
        int len = Math.max(8, Math.min(args.optInteger(0, 16), 64));
        byte[] bytes = new byte[len];
        random.nextBytes(bytes);
        return new Object[]{HexFormat.of().formatHex(bytes)};
    }
}
