package worldBuilder;

/**
 * a Bidder is an engine that chooses one of many contenders,
 * based on weighted bids.  It was created to choose tiles 
 * appropriate to a grid location.
 */

public class Bidder {
	
	private int numBidders;	// number of bidders 
	private int bidders[];	// registered bidders
	private int bids[];		// corresponding bids
	private int totBids;	// sum of bids
	
	/**
	 * instantiate a Bidder for a new auction
	 * @param maxBidders ... maximum number of bids in this auction
	 */
	public Bidder(int maxBidders) {
		bidders = new int[maxBidders];
		bids = new int[maxBidders];
		numBidders = 0;
		totBids = 0;
	}
	
	/**
	 * register a bid in the current auction
	 * 
	 * @param bidder
	 * @param bid
	 * @return	success/failure
	 */
	public boolean bid(int bidder, int bid) {
		if (bid <= 0)
			return true;	// zero bids never win
		if (numBidders >= bidders.length)
			return false;	// array overflow
		bidders[numBidders] = bidder;
		bids[numBidders++] = bid;
		totBids += bid;
		return true;
	}
	
	/**
	 * reset the bidder for a new set of bids
	 */
	public void reset() {
		numBidders = 0;
		totBids = 0;
	}
	
	/**
	 * determine the winner of the acution
	 * @param value ... random valut that chooses the winner
	 * @return
	 */
	public int winner(double value) {
		if (totBids == 0)
			return(0);
		else if (totBids == 1)
			return bidders[0];
		
		/*
		 * scale it up to a number > 10, take the fractional
		 * part, and use it to choose a number between 0 and tot-1
		 */
		if (value < 0)
			value = -value;
		else if (value == 0)	// this shouldn't happen
			value = Math.PI;
		while(value <= 10.0)
			value *= 10.0;
		value %= 1.0;
		value *= totBids;
		int chosen = (int) value;
		
		// scan the bids to see which one was selected
		for(int i = 0; i < numBidders; i++) {
			chosen -= bids[i];
			if (chosen < 0)
				return bidders[i];
		}
		System.err.println("ERROR: bid " + (int) value + "/" + totBids);
		return( 0 ); 	// no bidders or nobody won
	}
}