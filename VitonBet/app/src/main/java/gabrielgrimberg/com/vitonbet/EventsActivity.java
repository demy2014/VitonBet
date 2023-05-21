/***
 * Name: EventsActivity
 * Description: Activity that handles the events.
 * Notes: - Activity that holds the List View.
 *     - Allows user to create an event or place a bet.
 *     - Calls the fragments when a user want's to place a bet or create an event.
 *     - Adds the event details into the Database.
 *     - Retrieves the events from the Database to display to the user.
 **/

package gabrielgrimberg.com.vitonbet;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

class BetEvent {

    public String title;
    public String description;
}

public class EventsActivity extends AppCompatActivity implements
        EnterEvent.OnFragmentInteractionListener,
        ViewEvent.OnFragmentInteractionListener {

    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    static List<BetEvent> events = new ArrayList<>();

    public void onFragmentInteraction(Uri u) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        findViewById(R.id.enterEvent).setVisibility(View.GONE);
        findViewById(R.id.viewEvent).setVisibility(View.GONE);

        Helper.SetBalance(this);

        // Custom adapter.
        adapter = new CusAdapter(this, android.R.layout.simple_list_item_1, listItems);

        // ListView ID.
        final ListView lv = findViewById(R.id.list);

        lv.setAdapter(adapter);

        // When the list view item is clicked.
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                findViewById(R.id.viewEvent).setVisibility(View.VISIBLE);
                findViewById(R.id.list).setVisibility(View.GONE);
                findViewById(R.id.addNew).setVisibility(View.GONE);


                String s = (String) adapterView.getItemAtPosition(i);
                Log.d("mytag", s);

                // Use HashMap. This will do for now.
                for (BetEvent be : events) {

                    if (be.title == s) {

                        EditText t = findViewById(R.id.betTitle2);
                        t.setText(be.title);
                        EditText d = findViewById(R.id.description);
                        d.setText(be.description);
                    }
                }

            }
        });

        Button addNewBtn = findViewById(R.id.addNew);

        addNewBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                findViewById(R.id.enterEvent).setVisibility(View.VISIBLE);
                findViewById(R.id.list).setVisibility(View.GONE);
                findViewById(R.id.addNew).setVisibility(View.GONE);
            }
        });

        Button submit = findViewById(R.id.submit);

        submit.setOnClickListener(new View.OnClickListener() {

            // Adding to the database when the user creates an event.
            @Override
            public void onClick(View v) {

                // FirebaseAuth xAuth = FirebaseAuth.getInstance();
                DatabaseReference xDatabase = FirebaseDatabase.getInstance().getReference().child("Events");

                EditText title = findViewById(R.id.title);
                EditText description = findViewById(R.id.description);
                EditText note = findViewById(R.id.note);
                xDatabase.child(title.getText().toString()).child("description").setValue(description.getText().toString());
                xDatabase.child(title.getText().toString()).child("note").setValue(note.getText().toString());

                findViewById(R.id.enterEvent).setVisibility(View.GONE);
                findViewById(R.id.list).setVisibility(View.VISIBLE);
                findViewById(R.id.addNew).setVisibility(View.VISIBLE);

                Toast.makeText(EventsActivity.this, "Your submission will be reviewed by an admin.",
                        Toast.LENGTH_LONG).show();
            }
        });

        Button submitBet = findViewById(R.id.placeBet);

        // When the user tries to place a bet.
        submitBet.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                findViewById(R.id.viewEvent).setVisibility(View.GONE);
                findViewById(R.id.list).setVisibility(View.VISIBLE);
                findViewById(R.id.addNew).setVisibility(View.VISIBLE);


                String betAmount = ((EditText) findViewById(R.id.betAmount)).getText().toString();
                String title = ((EditText) findViewById(R.id.betTitle2)).getText().toString();

                // Error checking to allow user to place bet only if the user has enough funds.
                if (Integer.parseInt(betAmount) > Integer.parseInt(((TextView) findViewById(R.id.balance))
                        .getText()
                        .toString()
                        .replace("€", "")
                        .replace("Balance: ", ""))) {

                    Toast.makeText(EventsActivity.this, "You don't have enough balance.",
                            Toast.LENGTH_LONG).show();

                    return;
                }

                Toast.makeText(EventsActivity.this, "You will get notified when the result of the event is out.",
                        Toast.LENGTH_LONG).show();

                // RNG embedded class to determine the outcome of the bet placed.
                class BetResult extends AsyncTask<String, Void, Void> {

                    AppCompatActivity a;
                    boolean succ = false;
                    String eventTitle;
                    int betAmount;

                    public BetResult(AppCompatActivity a) { this.a = a; }

                    protected Void doInBackground(String... event) {

                        try {

                            eventTitle = event[0];
                            betAmount = Integer.parseInt(event[1]);
                            Thread.sleep(20000);

                        } catch (InterruptedException e) {

                            // Some error.
                        }

                        return null;
                    }

                    protected void onPostExecute(Void unused) {

                        Random r = new Random();

                        if (r.nextBoolean()) {

                            Toast.makeText(a, "You won your bet on " + eventTitle + " and won " + Integer.toString(betAmount * 2) + " euro.",
                                    Toast.LENGTH_LONG).show();

                            Helper.AddBalance(a, betAmount * 2);

                        } else {

                            Toast.makeText(a, "You lost your bet on " + eventTitle,
                                    Toast.LENGTH_LONG).show();
                            Helper.AddBalance(a, -betAmount);
                        }

                        DatabaseReference event = FirebaseDatabase.getInstance().getReference().child("Events").child(eventTitle);
                        event.removeValue();

                        events.removeIf(new Predicate<BetEvent>() {

                            @Override
                            public boolean test(BetEvent betEvent) {

                                if (betEvent.title.equals(eventTitle)) return true;
                                return false;
                            }
                        });
                        adapter.notifyDataSetChanged();
                    }
                }

                BetResult br = new BetResult(EventsActivity.this);
                br.execute(title, betAmount);

            }
        });

        DatabaseReference db = FirebaseDatabase.getInstance().getReference()
                .child("Events");

        db.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot event : dataSnapshot.getChildren()) {

                    addItems(event.getKey());
                    BetEvent be = new BetEvent();
                    be.title = event.getKey();
                    be.description = ((HashMap<String, String>) event.getValue()).get("description");
                    events.add(be);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // ...
            }
        });
    }

    public void addItems(String s) {

        listItems.add(s);
        adapter.notifyDataSetChanged();
    }

    class CusAdapter extends ArrayAdapter<String> implements View.OnClickListener {

        CusAdapter(Context c, int x, ArrayList<String> s) { super(c, x, s); }

        @Override
        public void onClick(View v) {

            int position = (Integer) v.getTag();
            Object object = getItem(position);
            String teams = (String) object;
            Log.d("mytag", teams);
        }
    }

}