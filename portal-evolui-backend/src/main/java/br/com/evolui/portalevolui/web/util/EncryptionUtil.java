package br.com.evolui.portalevolui.web.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

public class EncryptionUtil {
    static String IV = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0";
    static String KEY = "L9YKaYJJwCMxmBao";

    public static String generateToken(String value) throws Exception {
        return generateToken(KEY, value);
    }
    public static String generateToken(String key, String value) throws Exception {
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String now = df.format(new Date());
        return encrypt(value + ";" + now , key);
    }

    public static String getFromToken(String token) throws Exception {
        return getFromToken(token, KEY,null);
    }

    public static String getFromToken(String token, Long tolerance) throws Exception {
        return getFromToken(token, KEY,tolerance);
    }

    public static String getFromToken(String token, String key) throws Exception {
        return getFromToken(token, key,null);
    }

    public static String getFromToken(String token, String key, Long tolerance) throws Exception {
        return validarToken(token, tolerance, key);
    }

    private static String encrypt(String textopuro, String chave) throws Exception {
        for (int i = 0; i < textopuro.length() % 16; i++) {
            textopuro = textopuro + "\0";
        }
        Cipher encripta = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
        SecretKeySpec key = new SecretKeySpec(chave.getBytes("UTF-8"), "AES");
        encripta.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV.getBytes("UTF-8")));
        return byteToHex(encripta.doFinal(textopuro.getBytes("UTF-8")));
    }

    private static String decrypt(String hex_textoencriptado, String chave) throws Exception {
        try {
            byte[] textoencriptado = hexStringToByteArray(hex_textoencriptado);
            Cipher decripta = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
            SecretKeySpec key = new SecretKeySpec(chave.getBytes("UTF-8"), "AES");
            decripta.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV.getBytes("UTF-8")));
            String ret = new String(decripta.doFinal(textoencriptado), "UTF-8");
            if (ret.contains("\0")) {
                ret = ret.substring(0, ret.indexOf("\0"));
            }
            return ret;
        } catch (Exception ex) {
            throw new Exception("Chave errada!");
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    private static String validarToken(String token, Long toleranciaSegundos, String key) throws Exception {
        String infos[] = decrypt(token, key).split(";");
        if (infos.length != 2)
            throw new Exception("Token inválido");

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = df.parse(infos[1]);
        if (toleranciaSegundos != null) {
            // Get the date today using Calendar object.
            Date today = Calendar.getInstance().getTime();
            long seconds = (date.getTime() - today.getTime()) / 1000;
            if (seconds < 0)
                seconds = seconds * (-1);

            if (seconds > toleranciaSegundos)
                throw new Exception("Token inválido. Acerte sua data/hora");
        }

        return infos[0];
    }

}
