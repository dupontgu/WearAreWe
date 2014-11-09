package com.example.guyintrepid.hackday;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class UdpTestActivity extends Activity {

    public Socket sender;
    public BufferedReader br;
    public PrintStream bw;
    private int MAX_UDP_DATAGRAM_LEN = 1024;
    private int UDP_SERVER_PORT = 4545;
    private String TAG = "TAG";
    GoogleApiClient mGoogleApiClient;
    private Button b;
    private Node mainNode;


    private int LIST_COUNT_BYTE = 9;
    private byte INT_FLAG_BYTE = 105;

    class SocketListener implements Runnable {

        byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
        DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
        DatagramSocket ds = null;

        public void run() {
            while (true) {
                try {
                    if (ds == null) {
                        ds = new DatagramSocket(UDP_SERVER_PORT);
                    }
                    int listLength = 0;
                    ds.receive(dp);
                    //start at fixed byte, we know what the data is going to look like
                    byte currentByte = lMsg[LIST_COUNT_BYTE];
                    // while(currentByte == the letter i, for integer) - this is Max formatted list data
                    while (currentByte == INT_FLAG_BYTE) {
                        //how many ints?
                        listLength++;
                        currentByte = lMsg[LIST_COUNT_BYTE + listLength];
                    }

                    //at this point, the actual note data is scattered between some 0's
                    final List<Byte> byteList = new ArrayList<Byte>();
                    int listIndex = LIST_COUNT_BYTE + listLength;
                    //keep digging through bytes until we get as many notes as we were promised
                    for (int i = 0; i < listLength;) {
                        byte b = lMsg[listIndex];
                        if (b != 0) {
                            byteList.add(b);
                            i++;
                        }
                        listIndex++;
                    }

                    //tell watch to display chord with the notes we gathered
                    new NodeTask().execute(chordParse(byteList));
                    Thread.sleep(100);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    //use bit flags to build chords
    int C = 1;
    int Db = 2;
    int D = 4;
    int Eb = 8;
    int E = 16;
    int F = 32;
    int Gb = 64;
    int G = 128;
    int Ab = 256;
    int A = 512;
    int Bb = 1024;
    int B = 2048;

    int[] noteArray = {C, Db, D, Eb, E, F, Gb, G, Ab, A, Bb, B};
    HashMap<Integer, String> chordMap = new HashMap<Integer, String>();

    void fillChordMap() {
        chordMap.put(C | E | G, "C major");
        chordMap.put(C | Eb | G, "C minor");
        chordMap.put(Db | F | Ab, "C# major");
        chordMap.put(Db | E | Ab, "C# minor");
        chordMap.put(D | Gb | A, "D major");
        chordMap.put(D | F | A, "D minor");
        chordMap.put(Eb | G | Bb, "Eb major");
        chordMap.put(Eb | Gb | Bb, "Eb minor");
        chordMap.put(E | Ab | B, "E major");
        chordMap.put(E | G | B, "E minor");
        chordMap.put(F | A | C, "F major");
        chordMap.put(F | Ab | C, "F minor");
        chordMap.put(Gb | Bb | Db, "F# major");
        chordMap.put(Gb | A | Db, "F# minor");
        chordMap.put(G | B | D, "G major");
        chordMap.put(G | Bb | D, "G minor");
        chordMap.put(Ab | C | Eb, "G# major");
        chordMap.put(Ab | B | Eb, "G# minor");
        chordMap.put(A | Db | E, "A major");
        chordMap.put(A | C | E, "A minor");
        chordMap.put(Bb | D | F, "Bb major");
        chordMap.put(Bb | Db | F, "Bb minor");
        chordMap.put(B | Eb | Gb, "B major");
        chordMap.put(B | D | Gb, "B minor");
    }

    private String fetchChord(int code) {
        String chord = chordMap.get(code);
        return chord == null ? "" : chord;
    }

    //build chord from note values
    private String chordParse(List<Byte> bytes) {
        int chordValue = 0;
        for (byte b : bytes) {
            chordValue |= noteArray[(b % 12)];
        }
        return fetchChord(chordValue);
    }

    //Not using this, but I want to save it for later
    /*
    class SocketSender implements Runnable{
        String udpMsg = "hello world from UDP client " + UDP_SERVER_PORT;
        DatagramSocket ds = null;
        @Override
        public void run() {
            try {
                ds = new DatagramSocket();
                InetAddress serverAddr = InetAddress.getByName("192.168.43.24");
                DatagramPacket dp;
                dp = new DatagramPacket(udpMsg.getBytes(), udpMsg.length(), serverAddr, UDP_SERVER_PORT);
                ds.send(dp);
            } catch (SocketException e) {
                e.printStackTrace();
            }catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (ds != null) {
                    ds.close();
                }
            }
        }
    }
    */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp_test);
        //build chord "database"
        fillChordMap();
        TextView textView = (TextView) findViewById(R.id.ip_text);
        textView.setText(getIPAddress(true));
        b = (Button) findViewById(R.id.button);

        //if button is clicked, send the message TEST to the watch
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NodeTask task = new NodeTask();
                task.execute("TEST");
            }
        });

        //start listening for UDP
        Thread thread = new Thread(new SocketListener());
        thread.start();

        //Connect to wearable api
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);

                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.udp_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //get the IP address to plug into Max
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim < 0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

    //find a Node, pass on message
    public class NodeTask extends AsyncTask<String, Void, Void> {
        String header = "ww/ww/ww";

        @Override
        protected Void doInBackground(String... voids) {
            String message = voids[0];
            if (mainNode == null) {
                mainNode = getNodes(mGoogleApiClient).get(0);
            }
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, mainNode.getId(), header, message.getBytes()).await();
            if (!result.getStatus().isSuccess()) {
                Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
            }
            return null;
        }
    }

    private List<Node> getNodes(GoogleApiClient mGoogleApiClient) {
        ArrayList<Node> results = new ArrayList<Node>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node);
            Log.d("node", node.getId());
        }
        return results;
    }
}
