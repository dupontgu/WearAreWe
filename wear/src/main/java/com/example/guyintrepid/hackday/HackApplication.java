package com.example.guyintrepid.hackday;

import android.app.Application;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by GuyIntrepid on 10/3/14.
 */
public class HackApplication extends Application {

    public interface MessageInterface{
        public void onSuccess(boolean b);
    }

    static GoogleApiClient googleApiClient;
    static Node node;
    @Override
    public void onCreate()
    {
        //Connect watch to wearable api
        super.onCreate();
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        new NodeTask().execute();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();
    }

    public class NodeTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            setupNode();
            return null;
        }
    }

    private void setupNode(){
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
        if(nodes.getNodes() != null && !nodes.getNodes().isEmpty()){
            node = nodes.getNodes().get(0);
        }
    }

    //Nothing below is used currently - this is to send message back to phone

    public static void sendMessage(String path, String data){
        new SendMessageTask().execute(path, data);
    }

    public static void sendMessage(String path, String data, MessageInterface messageInterface){
        SendMessageTask sendMessageTask = new SendMessageTask();
        sendMessageTask.setMessageInterface(messageInterface);
        sendMessageTask.execute(path, data);
    }

    public static class SendMessageTask extends AsyncTask<String, Void, Boolean> {

        MessageInterface messageInterface;

        @Override
        protected Boolean doInBackground(String... strings) {
            Log.d("api client", Boolean.toString(googleApiClient == null));
            Log.d("api client", Boolean.toString(node == null));
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    googleApiClient, node.getId(), strings[0], strings[1].getBytes()).await();
            if (!result.getStatus().isSuccess()) {
                Log.e("t", "ERROR: failed to send Message: " + result.getStatus());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);
            if(messageInterface != null){
                messageInterface.onSuccess(aVoid);
            }
        }

        public void setMessageInterface(MessageInterface messageInterface) {
            this.messageInterface = messageInterface;
        }
    }

}
