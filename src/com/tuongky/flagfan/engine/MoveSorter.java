package com.tuongky.flagfan.engine;

import static com.tuongky.flagfan.engine.Evaluator.*;

public class MoveSorter {

	/*
	 *  ORDER:
	 *  1. Hash Table
	 *  2. Good Captures (SEE>=0) MVV/LVA
	 *  3. Killer moves (non-capture)
	 *  4. Non-Captures History Table
	 *  5. Bad Captures (SEE<0)
	 */
	
	final static int MOVE_SORT_RANGE = 7;
	
	Position p;
	int[] moveList;
	int num;
	long[][] historyTable;
	int moveIndex;
	
	public MoveSorter(Position p, int[] moveList, int num, long[][] historyTable) {
		this.p = p;
		this.moveList = moveList;
		this.num = num;
		this.historyTable = historyTable;
		moveIndex = 0;
	}
	
	public int nextMove() {
		if (moveIndex>MOVE_SORT_RANGE) return moveList[moveIndex++];
		long max = Long.MIN_VALUE;
		int index = num;
		int src, dst, captured;
		for (int i=moveIndex; i<num; i++) {
			src = moveList[i]>>8&0xff;
			dst = moveList[i]&0xff;
			captured = p.board[dst];
			long value = historyTable[src][dst];
			if (captured!=0) {
				int attacker = p.board[src];
				value += (PIECE_VALUES[captured]<<50)+(PIECE_VALUES[attacker]<<40); // MVV-LVA
			}
			if (value>max) {
				max = value;
				index = i;
			}
		}
		if (index>moveIndex) {
			int tmp = moveList[moveIndex];
			moveList[moveIndex] = moveList[index];
			moveList[index] = tmp;
		}
		return moveList[moveIndex++];		
	}
	
}
