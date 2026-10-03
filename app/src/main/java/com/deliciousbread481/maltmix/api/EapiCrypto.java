package com.deliciousbread481.maltmix.api;  
  
import org.json.JSONObject;  
  
import java.security.MessageDigest;  
import java.util.Map;  
  
import javax.crypto.Cipher;  
import javax.crypto.spec.SecretKeySpec;  
  
public class EapiCrypto {  
  
    private static final byte[] AES_KEY = "e82ckenh8dichen8".getBytes();  
    private static final String SEPARATOR = "-36cd479b6b5-";  
  
    /**  
     * @param fullUrl 完整请求 URL（含 /eapi/ 路径），内部会换回 /api/ 参与签名  
     * @param payload 业务参数（字符串值）  
     */  
    public static String encryptParams(String fullUrl, Map<String, String> payload) {  
        try {  
            String urlPath = new java.net.URL(fullUrl).getPath()  
                    .replace("/eapi/", "/api/");  
            JSONObject json = new JSONObject(payload);  
            String body = json.toString();  
  
            String signStr = "nobody" + urlPath + "use" + body + "md5forencrypt";  
            String digest = md5(signStr);  
            String plain = urlPath + SEPARATOR + body + SEPARATOR + digest;  
  
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");  
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(AES_KEY, "AES"));  
            return bytesToHex(cipher.doFinal(plain.getBytes("UTF-8")));  
        } catch (Exception e) {  
            throw new RuntimeException("eapi encrypt failed", e);  
        }  
    }  
  
    private static String md5(String s) throws Exception {  
        byte[] hash = MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8"));  
        return bytesToHex(hash);  
    }  
  
    private static String bytesToHex(byte[] bytes) {  
        StringBuilder sb = new StringBuilder(bytes.length * 2);  
        for (byte b : bytes) sb.append(String.format("%02x", b));  
        return sb.toString();  
    }  
}