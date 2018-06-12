package com.ocr.youtu;

import android.graphics.Bitmap;
import android.util.Base64;

import com.ocr.youtu.sign.YoutuSign;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;


/**
 * @author tyronetao
 */
public class Youtu {
    // 30 days
    private static int EXPIRED_SECONDS = 2592000;
    private String m_appid;
    private String m_secret_id;
    private String m_secret_key;
    private String m_end_point = "http://api.youtu.qq.com/youtu/";

    /**
     * Youtu 构造方法
     *
     * @param appid      授权appid
     * @param secret_id  授权secret_id
     * @param secret_key 授权secret_key
     */
    public Youtu(String appid, String secret_id, String secret_key) {
        m_appid = appid;
        m_secret_id = secret_id;
        m_secret_key = secret_key;
    }

    /**
     * bitmap转为base64
     *
     * @param bitmap
     * @return
     */
    public static String bitmapToBase64(Bitmap bitmap) throws IOException {

        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                throw e;
            }
        }
        return result;
    }

    private JSONObject SendHttpRequest(JSONObject postData, String mothod)
            throws IOException, JSONException, KeyManagementException, NoSuchAlgorithmException {

        StringBuffer mySign = new StringBuffer("");
        YoutuSign.appSign(m_appid, m_secret_id, m_secret_key,
                System.currentTimeMillis() / 1000 + EXPIRED_SECONDS,
                "", mySign);

        System.setProperty("sun.net.client.defaultConnectTimeout", "30000");
        System.setProperty("sun.net.client.defaultReadTimeout", "30000");
        URL url = new URL(m_end_point + mothod);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // set header
        connection.setRequestMethod("POST");
        connection.setRequestProperty("accept", "*/*");
        connection.setRequestProperty("user-agent", "youtu-android-sdk");
        connection.setRequestProperty("Authorization", mySign.toString());

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Content-Type", "text/json");
        connection.connect();

        // POST请求
        DataOutputStream out = new DataOutputStream(
                connection.getOutputStream());

        postData.put("app_id", m_appid);
        out.write(postData.toString().getBytes("utf-8"));
        out.flush();
        out.close();
        // 读取响应
        InputStream isss = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                isss));
        String lines;
        StringBuffer resposeBuffer = new StringBuffer("");
        while ((lines = reader.readLine()) != null) {
            lines = new String(lines.getBytes(), "utf-8");
            resposeBuffer.append(lines);
        }
        reader.close();
        // 断开连接
        connection.disconnect();

        JSONObject respose = new JSONObject(resposeBuffer.toString());

        return respose;

    }

    /*!
     * 人脸验证，给定一个Face和一个Person，返回是否是同一个人的判断以及置信度。
     *
     * @param bitmap 需要验证的人脸图片
     * @param person_id 验证的目标person
    */
    public JSONObject FaceVerify(Bitmap bitmap, String person_id)
            throws IOException, JSONException, KeyManagementException, NoSuchAlgorithmException {

        JSONObject data = new JSONObject();
        String imageData = bitmapToBase64(bitmap);
        data.put("image", imageData);
        data.put("person_id", person_id);
        JSONObject respose = SendHttpRequest(data, "api/faceverify");

        return respose;
    }

    /*!
     * 创建一个Person，并将Person放置到group_ids指定的组当中
     *
     * @param bitmap 需要新建的人脸图片
     * @param person_id 指定创建的人脸
     * @param group_ids 加入的group列表
    */
    public JSONObject NewPerson(Bitmap bitmap, String person_id,
                                List<String> group_ids) throws IOException, JSONException, KeyManagementException, NoSuchAlgorithmException {

        JSONObject data = new JSONObject();
        String imageData = bitmapToBase64(bitmap);
        data.put("image", imageData);
        data.put("person_id", person_id);
        data.put("group_ids", new JSONArray(group_ids));

        JSONObject respose = SendHttpRequest(data, "api/newperson");

        return respose;
    }

    /*!
     * 删除一个person下的face，包括特征，属性和face_id.
     *
     * @param person_id 待删除人脸的person ID
    */
    public JSONObject DelPerson(String person_id) throws IOException,
            JSONException, KeyManagementException, NoSuchAlgorithmException {

        JSONObject data = new JSONObject();

        data.put("person_id", person_id);

        JSONObject respose = SendHttpRequest(data, "api/delperson");

        return respose;
    }

    /*!
     * 设置Person的name.
     *
     * @param person_name 新的name
     * @param person_id 要设置的person id
    */
    public JSONObject SetInfo(String person_name, String person_id)
            throws IOException, JSONException, KeyManagementException, NoSuchAlgorithmException {
        JSONObject data = new JSONObject();

        data.put("person_name", person_name);
        data.put("person_id", person_id);
        JSONObject respose = SendHttpRequest(data, "api/setinfo");

        return respose;

    }

    /*!
     * 获取一个Person的信息, 包括name, id, tag, 相关的face, 以及groups等信息。
     *
     * @param person_id 待查询个体的ID
    */
    public JSONObject GetInfo(String person_id) throws IOException,
            JSONException, KeyManagementException, NoSuchAlgorithmException {
        JSONObject data = new JSONObject();

        data.put("person_id", person_id);
        JSONObject respose = SendHttpRequest(data, "api/getinfo");

        return respose;
    }

    /*!
     * 获取一个AppId下所有group列表
     */
    public JSONObject GetGroupIds() throws IOException, JSONException, KeyManagementException, NoSuchAlgorithmException {
        JSONObject data = new JSONObject();

        JSONObject respose = SendHttpRequest(data, "api/getgroupids");

        return respose;
    }

    /*!
     * 获取一个组Group中所有person列表
     *
     * @param group_id 待查询的组id
    */
    public JSONObject GetPersonIds(String group_id) throws IOException,
            JSONException, KeyManagementException, NoSuchAlgorithmException {
        JSONObject data = new JSONObject();

        data.put("group_id", group_id);
        JSONObject respose = SendHttpRequest(data, "api/getpersonids");

        return respose;
    }

    /*!
     * 获取一个组person中所有face列表
     *
     * @param person_id 待查询的个体id
    */
    public JSONObject GetFaceIds(String person_id) throws IOException,
            JSONException, KeyManagementException, NoSuchAlgorithmException {
        JSONObject data = new JSONObject();

        data.put("person_id", person_id);
        JSONObject respose = SendHttpRequest(data, "api/getfaceids");

        return respose;
    }

	/*!
     * 身份证OCR识别
	 *
	 * @param bitmap  输入图片
	 * @param cardType 身份证图片类型，0-正面，1-反面
	 */

    public JSONObject IdcardOcr(String bitmapBase64, int cardType) throws IOException,
            JSONException, KeyManagementException, NoSuchAlgorithmException {
        JSONObject data = new JSONObject();
        data.put("image", bitmapBase64);
        data.put("card_type", cardType);
        JSONObject response = SendHttpRequest(data, "ocrapi/idcardocr");
        return response;
    }

    /*!
     * 名片OCR识别
     *
     * @param bitmap  输入图片
     */
    public JSONObject NamecardOcr(String bitmapBase64) throws IOException,
            JSONException, KeyManagementException, NoSuchAlgorithmException {
        JSONObject data = new JSONObject();
        data.put("image", bitmapBase64);
        JSONObject response = SendHttpRequest(data, "ocrapi/namecardocr");
        return response;
    }
}
