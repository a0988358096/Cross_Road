package com.example.shimao.crossroad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static final String PREF = "H_SCORE";
    public static final String PREF_HighScore = "High_Score";
    int cur_on_road1 = 0;
    int cur_on_road2 = 0;
    int cur_on_road4 = 0;
    int cur_on_road6 = 0;  /* current car location on road */
    /* current man location */
    int cur_location_i = 7;
    int cur_location_j = 0;
    /* accident happen boolean */
    int gameStat = 1;   /* 1-game playing 2-collision or restart 3-pause*/
    /* game score */
    int Score = 0;
    int HighScore;
    TextView tv_gameSocre;
    Button leftB, upB, rightB, stopB;
    Button[][] buttons = new Button[8][6];
    int[][] id = {
            {R.id.button00, R.id.button01, R.id.button02, R.id.button03, R.id.button04, R.id.button05},
            {R.id.button10, R.id.button11, R.id.button12, R.id.button13, R.id.button14, R.id.button15},
            {R.id.button20, R.id.button21, R.id.button22, R.id.button23, R.id.button24, R.id.button25},
            {R.id.button30, R.id.button31, R.id.button32, R.id.button33, R.id.button34, R.id.button35},
            {R.id.button40, R.id.button41, R.id.button42, R.id.button43, R.id.button44, R.id.button45},
            {R.id.button50, R.id.button51, R.id.button52, R.id.button53, R.id.button54, R.id.button55},
            {R.id.button60, R.id.button61, R.id.button62, R.id.button63, R.id.button64, R.id.button65},
            {R.id.button70, R.id.button71, R.id.button72, R.id.button73, R.id.button74, R.id.button75}
    };
    int[][] stat = {
            /*
            0 -- safe area
            2 -- car position
            1 -- still safe area on the road
            3 -- man location
             */
            {0, 0, 0, 0, 0, 0},
            {2, 1, 1, 1, 1, 1},
            {2, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0},
            {2, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0},
            {2, 1, 1, 1, 1, 1},
            {3, 0, 0, 0, 0, 0}
    };
    int[][] initStat = {
            {0, 0, 0, 0, 0, 0},
            {2, 1, 1, 1, 1, 1},
            {2, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0},
            {2, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0},
            {2, 1, 1, 1, 1, 1},
            {3, 0, 0, 0, 0, 0}
    };

    Thread roadOne, roadTwo, roadFour, roadSix;

    //music
    private MediaPlayer mp;
    private boolean isStoped = true;

    //Sound
    private static final int SOUND_COUNT = 5;
    private int when_jump;
    private int when_dead;
    private int when_win;
    private SoundPool soundPool;

    private boolean collisionStat = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        restorePrefs();

        leftB = (Button) findViewById(R.id.left);
        leftB.setOnClickListener(leftListener);
        upB = (Button) findViewById(R.id.up);
        stopB = (Button) findViewById(R.id.stop);
        tv_gameSocre = (TextView) findViewById(R.id.tv_score);
        upB.setOnClickListener(upListener);
        rightB = (Button) findViewById(R.id.right);
        rightB.setOnClickListener(rightListener);
        stopB.setOnClickListener(stopListener);

        //Sound
        soundPool = new SoundPool(SOUND_COUNT, AudioManager.STREAM_MUSIC, 0);
        when_jump = soundPool.load(this, R.raw.flog, 1);
        when_dead = soundPool.load(this, R.raw.dead, 1);
        when_win = soundPool.load(this, R.raw.win, 1);

        roadOne = new roadOne();
        roadTwo = new roadTwo();
        roadFour = new roadFour();
        roadSix = new roadSix();
        roadOne.start();
        roadTwo.start();
        roadFour.start();
        roadSix.start();

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 6; j++) {
                buttons[i][j] = (Button) findViewById(id[i][j]);
            }
        }

        playMusic();
    }

    public void restorePrefs() {
        SharedPreferences setting = getSharedPreferences(PREF, 0);
        HighScore = setting.getInt(PREF_HighScore, 0);
    }

    private Handler mhandler = new Handler() {
        public void handleMessage(Message msg) {
            setMap();
            if (collisionStat) return;
            switch (msg.getData().getInt("road")) {
                case 1:
                    int location = msg.getData().getInt("location");
                    if (location != 5) {

                        int t1 = stat[1][location + 1];
                        stat[1][location + 1] = stat[1][location];
                        stat[1][location] = t1;
                    } else {
                        int t1 = stat[1][0];
                        stat[1][0] = stat[1][location];
                        stat[1][location] = t1;
                    }
                    break;
                case 2:
                    location = msg.getData().getInt("location");
                    if (location != 5) {

                        int t2 = stat[2][location + 1];
                        stat[2][location + 1] = stat[2][location];
                        stat[2][location] = t2;
                    } else {
                        int t1 = stat[2][0];
                        stat[2][0] = stat[2][location];
                        stat[2][location] = t1;
                    }
                    break;
                case 4:
                    location = msg.getData().getInt("location");
                    if (location != 5) {

                        int t4 = stat[4][location + 1];
                        stat[4][location + 1] = stat[4][location];
                        stat[4][location] = t4;
                    } else {
                        int t1 = stat[4][0];
                        stat[4][0] = stat[4][location];
                        stat[4][location] = t1;
                    }
                    break;
                case 6:
                    location = msg.getData().getInt("location");
                    if (location != 5) {

                        int t6 = stat[6][location + 1];
                        stat[6][location + 1] = stat[6][location];
                        stat[6][location] = t6;
                    } else {
                        int t1 = stat[6][0];
                        stat[6][0] = stat[6][location];
                        stat[6][location] = t1;
                    }
                    break;
            }
            super.handleMessage(msg);

            for (int i = 0; i < 8; i++) {

                Log.i("map", stat[i][0] + " " + stat[i][1] + " " + stat[i][2] + " " + stat[i][3] + " " + stat[i][4] + " " + stat[i][5]);

            }
            Log.i("map", "=========");
        }
    };


    /* update the view */
    private void setMap() {
        if (cur_location_i == 1) {
            if (cur_location_j == cur_on_road1) {
                gameStat = 2;
                stat[1][cur_on_road1] = 4;         /* collision , accident!!*/
                collisionDialog(1);
            }
        } else if (cur_location_i == 2) {
            if (cur_location_j == cur_on_road2) {
                gameStat = 2;
                stat[2][cur_on_road2] = 4;         /* collision , accident!!*/
                collisionDialog(2);
            }
        } else if (cur_location_i == 4) {
            if (cur_location_j == cur_on_road4) {
                gameStat = 2;
                stat[4][cur_on_road4] = 4;         /* collision , accident!!*/
                collisionDialog(4);
            }
        } else if (cur_location_i == 6) {
            if (cur_location_j == cur_on_road6) {
                gameStat = 2;
                stat[6][cur_on_road6] = 4;         /* collision , accident!!*/
                collisionDialog(6);
            }
        }
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 6; j++) {
                if (stat[i][j] == 2) {
                    if (i == 1)
                        buttons[i][j].setBackground(getResources().getDrawable(R.drawable.car1));
                    if (i == 2)
                        buttons[i][j].setBackground(getResources().getDrawable(R.drawable.car2));
                    if (i == 4)
                        buttons[i][j].setBackground(getResources().getDrawable(R.drawable.car3));
                    if (i == 6)
                        buttons[i][j].setBackground(getResources().getDrawable(R.drawable.car4));
                } else if (stat[i][j] == 1) {
                    buttons[i][j].setBackground(getResources().getDrawable(R.drawable.road));
                } else if (stat[i][j] == 3) {
                    if (i == 0 || i == 3 || i == 5 || i == 7)
                        buttons[i][j].setBackground(getResources().getDrawable(R.drawable.frog_on_grass));
                    if (i == 1 || i == 2 || i == 4 || i == 6)
                        buttons[i][j].setBackground(getResources().getDrawable(R.drawable.frog_on_road));
                } else if (stat[i][j] == 0) {
                    buttons[i][j].setBackground(getResources().getDrawable(R.drawable.grass));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.highscore) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("最高分為: " + HighScore + " 分")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
            return true;
        } else if (id == R.id.leave) {
            finish();
            return false;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        playMusic();//Play music
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopMusic();//Stop music
    }

    @Override
    public void onStop() {
        super.onStop();
        stopMusic();//Stop music
    }


    public View.OnClickListener leftListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            if (cur_location_j != 0 && gameStat != 3) {
                soundPool.play(when_jump, 1, 1, 0, 0, 1);//play sound
                int temp = stat[cur_location_i][cur_location_j - 1];
                stat[cur_location_i][cur_location_j - 1] = stat[cur_location_i][cur_location_j];
                stat[cur_location_i][cur_location_j] = temp;
                setMap();
                cur_location_j = cur_location_j - 1;
            }
        }
    };

    public View.OnClickListener upListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            if (cur_location_i != 0 && gameStat != 3) {
                soundPool.play(when_jump, 1, 1, 0, 0, 1);//play sound
                if (cur_location_i == 3 || cur_location_i == 5 || cur_location_i == 7) {
                    /* if move to the road */
                    stat[cur_location_i - 1][cur_location_j] = 3;
                    stat[cur_location_i][cur_location_j] = 0;
                    setMap();
                    cur_location_i = cur_location_i - 1;
                } else if (cur_location_i == 1 || cur_location_i == 4 || cur_location_i == 6) {
                    /*move to the safe area */
                    stat[cur_location_i - 1][cur_location_j] = 3;
                    stat[cur_location_i][cur_location_j] = 1;
                    setMap();
                    cur_location_i = cur_location_i - 1;
                } else {
                    /* move to still the road */
                    int temp = stat[cur_location_i - 1][cur_location_j];
                    stat[cur_location_i - 1][cur_location_j] = stat[cur_location_i][cur_location_j];
                    stat[cur_location_i][cur_location_j] = temp;
                    setMap();
                    cur_location_i = cur_location_i - 1;
                }
                if (gameStat != 2) {
                    Score += 5;
                    tv_gameSocre.setText(Score + "");
                }
                if (cur_location_i == 0) {
                    recordHighScore();
                    gameStat = 2;

                    stopMusic();
                    soundPool.play(when_win, 1, 1, 0, 0, 1);//play sound
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("遊戲結束!")
                            .setMessage("抵達終點! \n共獲得了: " + Score + "分")
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    initValue();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .show();
                }
            }
        }
    };

    public View.OnClickListener rightListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            if (cur_location_j != 5 && gameStat != 3) {
                soundPool.play(when_jump, 1, 1, 0, 0, 1);//play sound
                int temp = stat[cur_location_i][cur_location_j + 1];
                stat[cur_location_i][cur_location_j + 1] = stat[cur_location_i][cur_location_j];
                stat[cur_location_i][cur_location_j] = temp;
                setMap();
                cur_location_j = cur_location_j + 1;
            }
        }
    };

    class roadOne extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                while (true) {
                    if (gameStat == 1) {
                        Bundle bundle = new Bundle();
                        bundle.putInt("road", 1);
                        bundle.putInt("location", cur_on_road1);
                        Message m = new Message();
                        m.setData(bundle);
                        mhandler.sendMessage(m);
                        cur_on_road1++;
                        if (cur_on_road1 == 6) {
                            cur_on_road1 = 0;
                        }
                        Thread.sleep(500);                       /* 1s a move */
                    }
                    if (gameStat == 2) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class roadTwo extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                while (true) {
                    if (gameStat == 1) {
                        Bundle bundle = new Bundle();
                        bundle.putInt("road", 2);
                        bundle.putInt("location", cur_on_road2);
                        Message m = new Message();
                        m.setData(bundle);
                        mhandler.sendMessage(m);
                        cur_on_road2++;
                        if (cur_on_road2 == 6) {
                            cur_on_road2 = 0;
                        }
                        Thread.sleep(800);                      /* 3s a move */
                    }
                    if (gameStat == 2) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class roadFour extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                while (true) {
                    if (gameStat == 1) {
                        Bundle bundle = new Bundle();
                        bundle.putInt("road", 4);
                        bundle.putInt("location", cur_on_road4);
                        Message m = new Message();
                        m.setData(bundle);
                        mhandler.sendMessage(m);
                        cur_on_road4++;
                        if (cur_on_road4 == 6) {
                            cur_on_road4 = 0;
                        }
                        Thread.sleep(700);                     /* 4s a move */
                    }
                    if (gameStat == 2) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class roadSix extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                while (true) {
                    if (gameStat == 1) {
                        Bundle bundle = new Bundle();
                        bundle.putInt("road", 6);
                        bundle.putInt("location", cur_on_road6);
                        Message m = new Message();
                        m.setData(bundle);
                        mhandler.sendMessage(m);
                        cur_on_road6++;
                        if (cur_on_road6 == 6) {
                            cur_on_road6 = 0;
                        }
                        Thread.sleep(1000);                          /* 2s a move */
                    }
                    if (gameStat == 2) {
                        break;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void collisionDialog(int road) {
        if (collisionStat) return;
        collisionStat = true;
        soundPool.play(when_dead, 1, 1, 0, 0, 1);//play sound
        stopMusic();
        recordHighScore();
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.collision)
                .setMessage("發生車禍於第" + road + "路")
                .setCancelable(false)
                .setPositiveButton(R.string.restart, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        playMusic();
                        initValue();
                    }
                })
                .setNegativeButton(R.string.leave, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }

    public View.OnClickListener stopListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (gameStat != 3) {
                stopMusic();
                gameStat = 3;
                stopB.setText("RESUME");
            } else {
                playMusic();
                gameStat = 1;
                stopB.setText("STOP");
            }
        }
    };

    //----------------------   Music  ----------------------//
    private void playMusic() {
        if (mp == null || isStoped) {
            mp = create(MainActivity.this, R.raw.crazyfrog);
            isStoped = false;
        }
        mp.setLooping(true);
        mp.start();
    }

    private void stopMusic() {
        if (mp == null || isStoped)
            return;
        mp.stop();
        isStoped = true;

    }

    //Build environment for playing music
    public static MediaPlayer create(Context context, int resid) {
        AssetFileDescriptor afd;
        afd = context.getResources().openRawResourceFd(resid);
        if (afd == null)
            return null;
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            return mp;
        } catch (Exception e) {
            Log.e("Play music error!", e.toString());
            return null;
        }
    }

    public void recordHighScore() {
        if (Score > HighScore) {
            HighScore = Score;
        }
        SharedPreferences setting = getSharedPreferences(PREF, 0);
        SharedPreferences.Editor editor = setting.edit();
        editor.putInt(PREF_HighScore, HighScore);
        editor.commit();
    }

    public void initValue() {
        playMusic();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 6; j++) {
                stat[i][j] = initStat[i][j];
            }
        }
        Score = 0;
        tv_gameSocre.setText(Score + "");
        cur_on_road1 = 0;
        cur_on_road2 = 0;
        cur_on_road4 = 0;
        cur_on_road6 = 0;
        cur_location_i = 7;
        cur_location_j = 0;
        collisionStat = false;
        setMap();
        gameStat = 1;
        roadOne = new roadOne();
        roadOne.start();
        roadTwo = new roadTwo();
        roadTwo.start();
        roadFour = new roadFour();
        roadFour.start();
        roadSix = new roadSix();
        roadSix.start();
    }
}
