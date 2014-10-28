package tron;

import java.util.TimerTask;

public class Watchdog extends TimerTask {
	
	static public Watchdog createInfinite() {
		return new Watchdog() {
			@Override
			public void run() {}
			@Override
			public void check() {}
		};
	}

	static public Watchdog createDefault() {
		return new Watchdog();
	}
	
	@SuppressWarnings("serial")
	static public class Timeout extends Exception {
	}
	
	static final Timeout timeout= new Timeout();
	
	public boolean panic=false;
	
	@Override
	public void run() {
		panic=true;
	}
	
	public void check() throws Timeout {
		if(panic) throw timeout;
	}
	
	
	
}
