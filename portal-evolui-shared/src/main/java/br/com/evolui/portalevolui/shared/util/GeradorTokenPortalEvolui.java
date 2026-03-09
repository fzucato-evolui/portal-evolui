package br.com.evolui.portalevolui.shared.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

public class GeradorTokenPortalEvolui {

    private static final String DEFAULT_KEY = "KOfqT55eg0bU3mWS";
    private static final String DEFAULT_NAME = "portal_evolui";
    static String IV = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0";

    public static String generateToken() throws Exception {
        return generateToken(DEFAULT_NAME);
    }
    public static String generateToken(String value) throws Exception {
        return generateToken(value, DEFAULT_KEY);
    }

    public static String generateToken(String key, String value) throws Exception {
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String now = df.format(new Date());
        return encrypt(value + ";" + now , key);
    }

    public static String getFromToken(String token, String key) throws Exception {
        return validarToken(token, 180, key);
    }

    public static String encrypt(String textopuro) throws Exception {
        return encrypt(textopuro, DEFAULT_KEY);
    }
    public static String encrypt(String textopuro, String chave) throws Exception {
        int size = textopuro.getBytes("UTF-8").length;
        int rest = size % 16;
        if (rest > 0) {
            rest = 16 - rest;
        }
        for (int i = 0; i < rest; i++) {
            textopuro = textopuro + "\0";
        }
        Cipher encripta = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
        SecretKeySpec key = new SecretKeySpec(chave.getBytes("UTF-8"), "AES");
        encripta.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV.getBytes("UTF-8")));
        return byteToHex(encripta.doFinal(textopuro.getBytes("UTF-8")));
    }
    public static String decrypt(String hex_textoencriptado) throws Exception {
        return decrypt(hex_textoencriptado, DEFAULT_KEY);
    }
    public static String decrypt(String hex_textoencriptado, String chave) throws Exception {
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

    private static String validarToken(String token, long toleranciaSegundos, String key) throws Exception {
        String infos[] = decrypt(token, key).split(";");
        if (infos.length != 2)
            throw new Exception("Token inválido");

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = df.parse(infos[1]);
        // Get the date today using Calendar object.
        Date today = Calendar.getInstance().getTime();
        long seconds = (date.getTime() - today.getTime()) / 1000;
        if (seconds < 0)
            seconds = seconds * (-1);

        if (seconds > toleranciaSegundos)
            throw new Exception("Token inválido. Acerte sua data/hora");

        return infos[0];
    }

}
