package pt.ulisboa.tecnico.cmov.shopist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(myToolbar);

        //Termite Test - DELETE LATER
        SimWifiP2pBroadcast a = new SimWifiP2pBroadcast();
        SimWifiP2pManager mManager = null;
        SimWifiP2pManager.Channel mChanell = null;
        SimWifiP2pSocketServer mSrvSocket = null;
        SimWifiP2pSocket mCliSocket = null;
    }

    public void onClickSignUp(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    public void onClickNoAccount(View view) {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}