//package org.whispersystems.signalservice.internal.push;
//import android.os.AsyncTask;
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//public class httpUrlTest extends AsyncTask<String, Void, String> {
//
//  private static final String TAG = "HttpGetRequest";
//
//  @Override
//  protected String doInBackground(String... params) {
//    String urlString = params[0];
//    String result = "";
//    HttpURLConnection urlConnection = null;
//    try {
//      // 创建 URL 对象
//      URL url = new URL(urlString);
//
//      // 创建 HttpURLConnection 对象
//      urlConnection = (HttpURLConnection) url.openConnection();
//
//      // 设置请求方式为 GET
//      urlConnection.setRequestMethod("GET");
//
//      // 设置连接超时时间和读取超时时间
//      urlConnection.setConnectTimeout(5000);
//      urlConnection.setReadTimeout(5000);
//
//      // 连接服务器
//      urlConnection.connect();
//
//      // 读取服务器响应数据
//      BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
//      StringBuilder stringBuilder = new StringBuilder();
//      String line;
//      while ((line = reader.readLine()) != null) {
//        stringBuilder.append(line).append("\n");
//      }
//      reader.close();
//      result = stringBuilder.toString();
//    } catch (IOException e) {
//      // Log.e(TAG, "Error: " + e.getMessage());
//      System.out.println("ERROR:" + e.getMessage());
//    } finally {
//      if (urlConnection != null) {
//        urlConnection.disconnect();
//      }
//    }
//    return result;
//  }
//
//  @Override
//  protected void onPostExecute(String result) {
//    // 在这里处理服务器响应结果
//    // Log.d(TAG, "Response: " + result);
//    System.out.println("Response:" + result);
//  }
//}