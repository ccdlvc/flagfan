package com.tuongky.flagfan.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;

import com.tuongky.utils.FEN;
import com.tuongky.utils.FENException;
import com.tuongky.utils.MyTimer;

public class FlagFanEngine {

	final static String MY_NAME 	= "FlagFan 1.0";
	final static String AUTHOR 		= "Hieu Nguyen";
	final static String XBOARD 		= "xboard";
	final static String UCI 		= "uci";
	
    BufferedReader br;	
	PrintStream out;
	
	PrintWriter log = new PrintWriter("ff.log");

	Search search;
	Position p;
	
	int computer;
	int side;
	
	long maxTime;
	long timeLeft;
	long timeInc;
	int movesLeft;
	int maxMoves;
	int maxDepth;
	int gamePtr;
	
	void initAll() {
		maxTime = 10*1000; // 10s
		maxDepth = 50;		
		br = new BufferedReader(new InputStreamReader(System.in));
		out = new PrintStream(System.out, true);
		side = Piece.RED;
		computer = Piece.EMPTY;
		out.println("tellics say 	"+MY_NAME);
		out.println("tellics say 	by "+AUTHOR);
	}
	
	void mainLoop() throws IOException {
		String protocol = br.readLine();
		if (!protocol.equals(XBOARD)) {
			out.println("Cannot recognize the procotol "+protocol);
			return ;
		}
		String command;
		while (true) {
			
			if (side==computer) {
				play();
				continue;
			}
			
			String s = br.readLine().trim();
			
			log.println(s);
			log.flush();
			
			String[] ss = s.split(" ");
			command = ss[0].trim().toLowerCase();
			if (command.equals("protover")) {
				protover();
				continue;
			}
			if (command.equals("ping")) {
				ping(Integer.parseInt(ss[1]));
				continue;
			}
			if (command.equals("new")) {
				init();
				continue;
			}
			if (command.equals("quit")) {
				break;
			}
			if (command.equals("force")) {
				force();
				continue;
			}			
			if (command.equals("white")) {
				white();
				continue;
			}
			if (command.equals("black")) {
				black();
				continue;
			}
			if (command.equals("st")) {
				setTime(Integer.parseInt(ss[1]));
				continue;
			}
			if (command.equals("sd")) {
				setDepth(Integer.parseInt(ss[1]));
				continue;
			}
			if (command.equals("level")) {
				assert ss.length == 4;
				int mps, base, inc;
				mps = Integer.parseInt(ss[1]);
				inc = Integer.parseInt(ss[3]);
				if (ss[2].indexOf(":")>=0) {
					String[] tmp = ss[2].split(":");
					base = Integer.parseInt(tmp[0]) * 60 + Integer.parseInt(tmp[1]);
				} else base = Integer.parseInt(ss[2]);
				level(mps, base, inc);
				continue;
			}
			if (command.equals("time")) {
				int n = Integer.parseInt(ss[1]);
				time(n);
				continue;
			}
			if (command.equals("otim")) {
				int n = Integer.parseInt(ss[1]);
				otim(n);
				continue;
			}
			if (command.equals("go")) {
				go();
				continue;
			}
			if (command.equals("usermove")) {
				int r1, f1, r2, f2;
				r1 = (ss[1].charAt(1)-'0');
				f1 = (ss[1].charAt(0)-'a')+3;
				r2 = (ss[1].charAt(3)-'0');
				f2 = (ss[1].charAt(2)-'a')+3;
				r1 = 9-r1+3; r2 = 9-r2+3;
				int src, dst;
				src = (r1<<4)+f1;
				dst = (r2<<4)+f2;
				int move = (src<<8)+dst;
				usermove(move);
				continue;
			}
		}
	}
	
	void play() {
		int m = movesLeft<=0 ? 40 : movesLeft;
		long tlim = (long) ((0.6-0.06)*(timeLeft+(m-1)*timeInc)/(m+7));
		if (tlim>timeLeft/15) tlim = timeLeft/15;
		int bestMove = findNextMove();
		makemove(bestMove);
	}
	
	public FlagFanEngine() throws IOException {
		initAll();
		mainLoop();
	}
	
	public void showNextMove() {
		MyTimer timer = new MyTimer();
		System.out.println("Thinking...");
		int move = findNextMove();
		p.printMoveForHuman(move);
		double nps = Evaluator.getInstance().nodeCount/(timer.elapsedTime()*0.001)*1e-6;
		timer.printElapsedTime();
		System.out.println("Node Count = "+Evaluator.getInstance().nodeCount);
		System.out.println("Node Per Second = "+String.valueOf(nps).substring(0, 5)+" millions");
	}
	
	public int findNextMove() {
		return search.findBestMove();
	}
	
	public static void main(String[] args) throws IOException {
		new FlagFanEngine();
	}

	// GUI -> Engine
	
	void init() {
		FEN f;
		try {
			f = new FEN(FEN.START_FEN);
			p = new Position(f.getBoard90(), f.getTurn());
			search = new Search(p);		
		} catch (FENException e) {
			e.printStackTrace();
		}		
		side = Piece.RED;
		computer = Piece.BLACK;
		timeLeft = maxTime;
		movesLeft = maxMoves;
		gamePtr = 0;
	}
	
	void force() {
		computer = Piece.EMPTY;
	}
	
	void white() {
		side = Piece.RED;
		computer = Piece.BLACK;
	}
	
	void black() {
		side = Piece.BLACK;
		computer = Piece.RED;		
	}

	void setTime(int time) {
		timeLeft = maxTime = time * 1000;
		timeInc = 0;
		movesLeft = maxMoves = 1;
	}

	void setDepth(int depth) {
		maxDepth = depth;
	}

	void level(int mps, int base, int inc) {
		movesLeft = mps;
		timeLeft = maxTime = base * 1000;
		timeInc = inc * 1000; 
	}

	void time(int n) {
		timeLeft = n*10;
	}

	void otim(int n) {
		// do nohting
	}

	void protover() {
		feature("myname", "\""+MY_NAME+"\"");
		feature("usermove", "1");
		feature("memory", "0");
		feature("smp", "1");
        feature("setboard", "0");
        feature("ping", "1");
//        feature("done", "0");
        feature("variants", "\"xiangqi\"");        
        feature("done", "1");
	}

	void ping(int n) {
		pong(n);
	}
	
	void go() {
		computer = side;
		movesLeft -= (gamePtr + (side==Piece.RED?1:0))>>1;
		while (maxMoves>0 && movesLeft<=0) movesLeft += maxMoves;
	}
	
	void usermove(int move) {
		side ^= Piece.RED ^ Piece.BLACK;
		p.makeRealMove(move);
	}
	
	// Engine -> GUI

	void feature(String key, String value) {
		out.println("feature "+key+"="+value);		
	}

	void pong(int n) {
		out.println("pong "+n);
	}

	void makemove(int move) {
		int src, dst;
		src = (move>>8)&0xff;
		dst = move&0xff;
		char r1, f1, r2, f2;
		r1 = (char) ('0'+9-((src>>4)-3));
		f1 = (char) ('a'+(src&0xf)-3);
		r2 = (char) ('0'+9-((dst>>4)-3));
		f2 = (char) ('a'+(dst&0xf)-3);
		String ms = ""+f1+r1+f2+r2;
		out.println("move "+ms);
		side ^= Piece.RED ^ Piece.BLACK;
		p.makeRealMove(move);
	}
	
}