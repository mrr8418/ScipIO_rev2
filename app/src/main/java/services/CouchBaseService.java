package services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.RemoteRequestResponseException;
import com.couchbase.lite.replicator.Replication;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Matt on 3/23/2017.
 */
public class CouchBaseService extends Service {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private Manager databaseManager = null;
    private Database database = null;
    private String dbname = "test_syncgateway";
    private DatabaseOptions options = new DatabaseOptions();
    private String username = "default_user";
    private boolean authenticationStatus = false;

    private LiveQuery usernameListsLiveQuery = null;
    private LiveQuery numbersTypeListsLiveQuery = null;

    //SyncGateway
    URL url = null;
    private String syncGatewayUrl = "http://10.0.0.84:4984/test_syncgateway/";
    private Replication pusher;
    private Replication puller;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public CouchBaseService getService() {
            // Return this instance of LocalService so clients can call public methods
            return CouchBaseService.this;
        }
    }

    public void setUsername(String username){
        this.username = username;
    }

    public String getUsername(){
        return this.username;
    }

    public boolean isAuthenticationStatus() {
        return authenticationStatus;
    }

    public void setAuthenticationStatus(boolean authenticationStatus) {
        this.authenticationStatus = authenticationStatus;
    }

    @Override
    public IBinder onBind(Intent intent) {

        try {
            options.setCreate(true);
            databaseManager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            database = databaseManager.openDatabase(dbname,options);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        try {
            url = new URL(syncGatewayUrl);
            Log.v("URL","Created");
        } catch (MalformedURLException e) {
            Log.v("URL","failed");
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Database URL Failure", Toast.LENGTH_SHORT).show();
        }


        return mBinder;
    }

    /** method for clients */
    public void saveData(HashMap<String, Object> saveProperties){

        String docId = this.username + "." + UUID.randomUUID();
        Document document = database.getDocument(docId);

        try {
            document.putProperties(saveProperties);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }



        Log.d("app", String.format("Document ID :: %s", document.getId()));
        Log.d("app", String.format("Document Type: %s , data: %s", (String) document.getProperty("documentType"), (String) document.getProperty("data")));

    }

    public LiveQuery createUsernameViewLiveQuery(){

        //Going to need a queryEnumerator for this one
        View usernamesListsView = database.getView("list/listsByUsername");
        if(usernamesListsView.getMap() == null){
            usernamesListsView.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    if(document.get("username")!=null){
                        String usernameCheck = (String) document.get("username");
                        Log.v(usernameCheck, username);
                        if(username.equals(usernameCheck)){
                            emitter.emit(document.get("username"), document.get("data"));
                            Log.v(usernameCheck, username);
                        }
                    }
                }
            }, "1.0");
        }

       return usernameListsLiveQuery = usernamesListsView.createQuery().toLiveQuery();

    }

    public LiveQuery createNumbersTypeViewLiveQuery(){

        //Going to need a queryEnumerator for this one
        View usernamesListsView = database.getView("list/listsByNumbersType");
        if(usernamesListsView.getMap() == null){
            usernamesListsView.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    if(document.get("documentType")!=null){
                        String type = (String) document.get("documentType");
                        if("numbers".equals(type)){
                            emitter.emit(document.get("username"), document.get("data"));
                        }
                    }
                }
            }, "1.0");
        }

        return numbersTypeListsLiveQuery = usernamesListsView.createQuery().toLiveQuery();

    }



    public void stopReplication(){
        puller.stop();
        pusher.stop();
    }

    public void authenticateUser(final String username, String password){

        puller = database.createPullReplication(url);
        pusher = database.createPushReplication(url);
        Authenticator authenticator = AuthenticatorFactory.createBasicAuthenticator(username, password);
        setAuthenticationStatus(true);
        Log.v(username, password);
        pusher.setAuthenticator(authenticator);
        puller.setAuthenticator(authenticator);
        pusher.setContinuous(true);
        puller.setContinuous(true);
        pusher.start();
        puller.start();

        pusher.addChangeListener(new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
                if(event.getError() != null){

                    Throwable lastError = event.getError();
                    if(lastError instanceof RemoteRequestResponseException){
                        RemoteRequestResponseException exception = (RemoteRequestResponseException) lastError;
                        if(exception.getCode() == 401){
                            pusher.stop();
                            puller.stop();
                            setAuthenticationStatus(false);
                            Log.v("Status", String.valueOf(isAuthenticationStatus()));

                        }
                    }
                }
            }

        });


    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (usernameListsLiveQuery != null) {
            usernameListsLiveQuery.stop();
            usernameListsLiveQuery = null;
        }
    }
}
