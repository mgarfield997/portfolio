package blackjack;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JPanel;

import gameName.ButtonListener;

public class TablePanel extends JPanel{
/**
 * Author: Megan Garfield
 * Date Created: 8/30/2018
 * Last Updated: 9/11/2018
 */
	//Serialization
	private static final long serialVersionUID = 1L;
	
	/*****Constants*****/
	private static final int CARDWIDTH = Deck.getCardWidth();
	private static final int CARDHEIGHT = Deck.getCardHeight();
	
	private static final int SPACING = 4; //Space between cards
	private static final int MARGIN = 10;  //Margin around table
	private static final int WIDTH = 13*CARDWIDTH + 12*SPACING + 2*MARGIN; //width of table
	private static final int HEIGHT = 9*CARDHEIGHT + 3*MARGIN; //height of table
	
	private Deck deck;
	private CardStack playerStack = new CardStack(200, 300, CARDWIDTH+3);
	private CardStack dealerStack = new CardStack(200, 100, CARDWIDTH+3);
	private CardStack deckStack = new CardStack(220, 200, 0);

	
	public Dimension getPreferredSize() {
		Dimension size = new Dimension(WIDTH, HEIGHT);
		return size;
	}
	
	//constructor
	public TablePanel() {
		
		deck = new Deck();
		deal();
		
		//Add Deal button
		JButton buttonDeal = new JButton("Hit");
		buttonDeal.addActionListener(new ButtonListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				repaint();
			} //end actionPerformed
		}); //end addActionListener
		add(buttonDeal);
		
		//Add Stay Button
		JButton buttonStay = new JButton("Stay");
		add(buttonStay);
		
	} //end constructor
	
	//Deal the deck out
	private void deal() {
		
		//Deals players initial cards
		for(int playerDeck = 0; playerDeck < 2; playerDeck++) {
			Card card = deck.deal();
			playerStack.add(card);
		} //end player for
		
		//Deals dealers initial cards
		for(int dealerDeck = 0; dealerDeck < 2; dealerDeck++) {
			Card card = deck.deal();
			dealerStack.add(card);
		} //end dealer for
		
		//Deals rest of deck
		for(int deckS = 0; deckS < 48; deckS++) {
			Card card = deck.deal();
			deckStack.add(card);
		} //end deck for
		
		repaint();
	} //end deal
	
	//paint component
	public void paintComponent(Graphics g) {
		
		Graphics2D g2 = (Graphics2D) g;
		
		//draw Background
		g2.setColor(Color.GREEN);
		g2.fillRect(0, 0, WIDTH, HEIGHT);
		
		//draw board
		dealerStack.draw(g2);
		playerStack.draw(g2);
		deckStack.draw(g2);
		
	} //end paintComponent
	
	
	
}

