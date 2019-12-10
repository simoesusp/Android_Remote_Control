package com.github.simoesusp.takethecoverout;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public abstract class BaseDeviceControlActivity extends AppCompatActivity implements Runnable {
    protected volatile int l = 0;
    protected volatile int r = 0;
    protected volatile int vel = 0;
    protected volatile boolean flagTouchLeft = false;
    protected volatile boolean flagTouchRight = false;
    protected volatile boolean buttonClicked = false;
    protected volatile boolean fAuto = false;

    protected boolean backEnabled;
    protected double speedMultiplier;

    private Thread sendThread;

    protected abstract void sendMotorSpeed(boolean automatic, int l, int r);

    protected abstract int sendDelay();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar t = new Toolbar(this);
        t.setTitle("Controle Robô");
        setActionBar(t);

        setContentView(R.layout.controle_robo);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        backEnabled = prefs.getBoolean("enable-back", false);
        speedMultiplier = prefs.getInt("max-speed-percent", 25) / 100.0;

        initButtons();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initButtons() {

        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!buttonClicked) {
                    buttonClicked = true;
                    button.setBackgroundResource(R.drawable.btoff);
                    l = 0;
                    r = 0;
                    vel = 0;
                    sendThread = new Thread(BaseDeviceControlActivity.this);
                    sendThread.start();
                } else {
                    buttonClicked = false;
                    button.setBackgroundResource(R.drawable.bton);
                    sendThread.interrupt();
                    sendThread = null;
                    l = 0;
                    r = 0;
                    vel = 0;
                }
                send(false);
            }
        });

        Button btLeft = findViewById(R.id.left);
        btLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    flagTouchLeft = true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    flagTouchLeft = false;
                }
                send(false);
                return true;
            }
        });

        Button btRight = findViewById(R.id.right);
        btRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    flagTouchRight = true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    flagTouchRight = false;
                }
                send(false);
                return true;
            }
        });

        Button btFront = findViewById(R.id.front);
        btFront.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (vel >= 0 && vel < 5) {
                        vel++;
                    } else if (vel == 6) {
                        vel = 0;
                    } else if (vel > 6 && vel <= 10) {
                        vel--;
                    }
                }
                send(false);
                return true;
            }
        });

        Button btBack = findViewById(R.id.back);
        btBack.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (vel > 0 && vel <= 5) {
                        vel--;
                    } else if (vel == 0) {
                        //FINALLY fixed
                        if (!backEnabled) {
                            Toast.makeText(BaseDeviceControlActivity.this, R.string.back_broken, Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        vel = 6;
                    } else if (vel >= 6 && vel < 10) {
                        vel++;
                    }
                }
                send(false);
                return true;
            }
        });

        Button stopButton = findViewById(R.id.stop);
        stopButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    vel = 0;
                }
                send(false);
                return true;
            }
        });
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && !isDestroyed()) {
            try {
                Thread.sleep(sendDelay());
            } catch (InterruptedException e) {
                return;
            }

            send(true);
        }
    }

    public void onClickAuto(View v) {
        Toast.makeText(this, R.string.dont_do_that, Toast.LENGTH_SHORT).show();
        /*if (!fAuto) {
            fAuto = true;
        } else {
            fAuto = false;
            vel = r = l = 0;
        }*/
    }

    private void send(boolean automatic) {
		if (flagTouchLeft) {
			if (vel > 0 && vel <= 5) {
				if (l > 0) {
					l--;
				}
			} else if (vel >= 6 && vel <= 10) {
				if (l > 6) {
					l--;
				} else if (l == 6) {
					//l = 0;
					if(r == 6) r++;
				}
			}
		} else {
			l = vel;
			if (flagTouchRight) {
				if (vel > 0 && vel <= 5) {
					if (r > 0) {
						r--;
					}
				} else if (vel >= 6 && vel <= 10) {
					if (r > 6) {
						r--;
					} else if (r == 6) {
						//r = 0;
						if(l == 6) l++;
					}
				}
			} else {
				r = vel;
			}
		}

        if (vel == 0) {
            if (flagTouchLeft) {
                r = 1;
            }
            if (flagTouchRight) {
                l = 1;
            }
        }

        sendMotorSpeed(automatic, l, r);
    }
}
