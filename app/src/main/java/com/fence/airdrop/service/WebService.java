package com.fence.airdrop.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.fence.airdrop.observer.ApkPublisher;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;

public class WebService extends Service {

    private static final String ACTION_START_WEB_SERVICE = "com.fence.airdrop.action.START_WEB_SERVICE";
    private static final String ACTION_STOP_WEB_SERVICE = "com.fence.airdrop.action.STOP_WEB_SERVICE";

    private static final String CSS_CONTENT_TYPE = "text/css;charset=utf-8";
    private static final String JS_CONTENT_TYPE = "application/javascript";
    private static final String PNG_CONTENT_TYPE = "application/x-png";
    private static final String JPG_CONTENT_TYPE = "application/jpeg";
    private static final String SWF_CONTENT_TYPE = "application/x-shockwave-flash";
    private static final String WOFF_CONTENT_TYPE = "application/x-font-woff";
    private static final String TTF_CONTENT_TYPE = "application/x-font-truetype";
    private static final String SVG_CONTENT_TYPE = "image/svg+xml";
    private static final String EOT_CONTENT_TYPE = "image/vnd.ms-fontobject";
    private static final String MP3_CONTENT_TYPE = "audio/mp3";
    private static final String MP4_CONTENT_TYPE = "video/mpeg4";

    public static final int PORT = 8888;
    public static final String APK_DIR = Environment.getExternalStorageDirectory() + File.separator + "airdrop";

    private AsyncHttpServer mServer = new AsyncHttpServer();
    private AsyncServer mAsyncServer = new AsyncServer();
    private FileUploadHolder mUploadHolder = new FileUploadHolder();
    private FileHandler mHandler = new FileHandler(mUploadHolder);

    public static void start(Context context) {
        Intent intent = new Intent(context, WebService.class);
        intent.setAction(ACTION_START_WEB_SERVICE);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, WebService.class);
        intent.setAction(ACTION_STOP_WEB_SERVICE);
        context.startService(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_START_WEB_SERVICE.equals(action)) {
                startServer();
                loadResources();
                setQueryListener();
                setUploadListener();
                setDownloadListener();
                setDeleteListener();
            } else if (ACTION_STOP_WEB_SERVICE.equals(action)) {
                stopSelf();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startServer() {
        mServer.listen(mAsyncServer, PORT);
    }

    private void loadResources() {
        mServer.get("/images/.*", this::sendResources);
        mServer.get("/scripts/.*", this::sendResources);
        mServer.get("/css/.*", this::sendResources);

        mServer.get("/", (request, response) -> {
            try {
                response.send(getIndex());
            } catch (IOException e) {
                e.printStackTrace();
                response.code(500).end();
            }
        });
    }

    private String getIndex() throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(getAssets().open("airdrop/index.html"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int len;
            byte[] tmp = new byte[10240];
            while ((len = bis.read(tmp)) > 0) {
                baos.write(tmp, 0, len);
            }

            return new String(baos.toByteArray(), "utf-8");
        } catch (IOException exception) {
            exception.printStackTrace();
            throw exception;
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendResources(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
        try {
            String fullPath = request.getPath();
            fullPath = fullPath.replace("%20", " ");
            String resourceName = fullPath;
            if (resourceName.startsWith("/")) {
                resourceName = resourceName.substring(1);
            }
            if (resourceName.indexOf("?") > 0) {
                resourceName = resourceName.substring(0, resourceName.indexOf("?"));
            }
            if (!TextUtils.isEmpty(getContentTypeByResourceName(resourceName))) {
                response.setContentType(getContentTypeByResourceName(resourceName));
            }
            BufferedInputStream bInputStream = new BufferedInputStream(getAssets().open("airdrop/" + resourceName));
            response.sendStream(bInputStream, bInputStream.available());
        } catch (IOException e) {
            e.printStackTrace();
            response.code(404).end();
            return;
        }
    }

    private String getContentTypeByResourceName(String resourceName) {
        if (resourceName.endsWith(".css")) {
            return CSS_CONTENT_TYPE;
        } else if (resourceName.endsWith(".js")) {
            return JS_CONTENT_TYPE;
        } else if (resourceName.endsWith(".swf")) {
            return SWF_CONTENT_TYPE;
        } else if (resourceName.endsWith(".png")) {
            return PNG_CONTENT_TYPE;
        } else if (resourceName.endsWith(".jpg") || resourceName.endsWith(".jpeg")) {
            return JPG_CONTENT_TYPE;
        } else if (resourceName.endsWith(".woff")) {
            return WOFF_CONTENT_TYPE;
        } else if (resourceName.endsWith(".ttf")) {
            return TTF_CONTENT_TYPE;
        } else if (resourceName.endsWith(".svg")) {
            return SVG_CONTENT_TYPE;
        } else if (resourceName.endsWith(".eot")) {
            return EOT_CONTENT_TYPE;
        } else if (resourceName.endsWith(".mp3")) {
            return MP3_CONTENT_TYPE;
        } else if (resourceName.endsWith(".mp4")) {
            return MP4_CONTENT_TYPE;
        }
        return "";
    }

    private void setUploadListener() {
        mServer.post("/files", (request, response) -> {
                MultipartFormDataBody body = (MultipartFormDataBody) request.getBody();

                body.setMultipartCallback(part -> {
                    if (part.isFile()) {
                        body.setDataCallback((emitter, list) -> {
                            mUploadHolder.write(list.getAllByteArray());
                            list.recycle();
                        });
                    } else {
                        if (body.getDataCallback() == null) {
                            body.setDataCallback((emitter, list) -> {
                                try {
                                    String fileName = URLDecoder.decode(new String(list.getAllByteArray()), "UTF-8");
                                    mUploadHolder.setFileName(fileName);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }

                                list.recycle();
                            });
                        }
                    }
                });

                request.setEndCallback((Exception e) -> {
                    mHandler.sendEmptyMessage(0);
                    mUploadHolder.reset();
                    response.end();
                });
            }
        );
    }

    private void setQueryListener() {
        mServer.get("/files", (request, response) -> {
            JSONArray array = new JSONArray();

            File dir = new File(APK_DIR);
            if (!dir.exists() || !dir.isDirectory()) {
                return;
            }

            String[] fileNames = dir.list();
            if (fileNames == null) {
                return;
            }

            for (String fileName : fileNames) {
                File file = new File(dir, fileName);

                if (!file.exists() || !file.isFile()) {
                    continue;
                }

                try {
                    DecimalFormat df = new DecimalFormat("0.00");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", fileName);
                    long fileLen = file.length();

                    if (fileLen > 1024 * 1024) {
                        jsonObject.put("size", df.format(fileLen * 1f / 1024 / 1024) + "MB");
                    } else if (fileLen > 1024) {
                        jsonObject.put("size", df.format(fileLen * 1f / 1024) + "KB");
                    } else {
                        jsonObject.put("size", fileLen + "B");
                    }
                    array.put(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            response.send(array.toString());
        });
    }

    private void setDownloadListener() {
        mServer.get("/files/.*", (request, response) -> {
            String path = request.getPath().replace("/files/", "");

            try {
                path = URLDecoder.decode(path, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            File file = new File(APK_DIR, path);
            if (file.exists() && file.isFile()) {
                try {
                    response.getHeaders().add("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.getName(), "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                response.sendFile(file);
                return;
            }

            response.code(404).send("Not found!");
        });
    }

    private void setDeleteListener() {
        mServer.post("/files/.*", (request, response) -> {
            final UrlEncodedFormBody body = (UrlEncodedFormBody) request.getBody();

            if ("delete".equalsIgnoreCase(body.get().getString("_method"))) {
                String path = request.getPath().replace("/files/", "");

                try {
                    path = URLDecoder.decode(path, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                File file = new File(APK_DIR, path);
                if (file.exists() && file.isFile()) {
                    file.delete();

                    Message message = mHandler.obtainMessage(-1);
                    message.obj = file;
                    mHandler.sendMessage(message);
                }
            }

            response.end();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mServer != null) {
            mServer.stop();
        }

        if (mAsyncServer != null) {
            mAsyncServer.stop();
        }
    }

    public class FileUploadHolder {

        private String mFileName;
        private File mFile;
        private BufferedOutputStream mOutputStream;
        private long mTotalSize;

        public void setFileName(String fileName) {
            mFileName = fileName;
            mTotalSize = 0;

            File dir = new File(APK_DIR);

            if (!dir.exists()) {
                dir.mkdirs();
            }

            mFile = new File(APK_DIR, mFileName);
            try {
                mOutputStream = new BufferedOutputStream(new FileOutputStream(mFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void reset() {
            if (mOutputStream != null) {
                try {
                    mOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mOutputStream = null;
        }

        public void write(byte[] data) {
            if (mOutputStream != null) {
                try {
                    mOutputStream.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mTotalSize += data.length;
        }
    }

    private static class FileHandler extends Handler {

        private FileUploadHolder mHolder;

        public FileHandler(FileUploadHolder holder) {
            mHolder = holder;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) { // upload
                ApkPublisher.getInstance().setDeleteApk(false);
                ApkPublisher.getInstance().notify(mHolder.mFile);
            } else if (msg.what == -1) { // delete
                File file = (File) msg.obj;
                ApkPublisher.getInstance().setDeleteApk(true);
                ApkPublisher.getInstance().notify(file);
            }
        }
    }
}
