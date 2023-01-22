import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@ServerEndpoint("/gamechat")
public class GameChat {
    static Set<Session> players = Collections.synchronizedSet(new HashSet<Session>());
    @OnOpen
    public void handleOpen(Session userSession){
        players.add(userSession);
    }

    @OnMessage
    public void handleMessage(String message, Session userSession) throws IOException {
        String username = (String) userSession.getUserProperties().get("username");
        if(username == null){
            userSession.getUserProperties().put("username",message);
            userSession.getBasicRemote().sendText(buildJsonData("System","you are now connected as "+message));
        }else{
            Iterator<Session> iterator = players.iterator();
            ServerThread st = new ServerThread();
            ClientThread ct = new ClientThread(message);
            FutureTask ft = new FutureTask<String>(st);
            Thread t = new Thread(ft);
            ct.start();
            t.start();

            String msg = "";
            try {
                msg = (String) ft.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            while(iterator.hasNext()) iterator.next().getBasicRemote().sendText(buildJsonData(username,msg));
        }
    }


    private String buildJsonData(String username, String message) {
        JsonObject jsonObject = Json.createObjectBuilder().add("message", username+": "+message).build();
        StringWriter stringWriter = new StringWriter();
        try(JsonWriter jsonWriter = Json.createWriter(stringWriter)){jsonWriter.writeObject(jsonObject);}
        return stringWriter.toString();
    }

    @OnClose
    public void handleClose(Session userSession){
        players.remove(userSession);
    }
}
