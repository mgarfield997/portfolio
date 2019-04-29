package blackjack;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

public class Card {
/**
 * Author: Megan Garfield
 * Date Created: 9/4/2018
 * Last Updated: 9/11/2018
 */
	
	/*****Variables*****/
	private String rank = "";
	private int suit = -1;
	private int value = 0;
	private Image img = null;
	private static int width = 0;
	private static int height = 0;
	private int x = 0;
	private int y = 0;
	
	//Constructor
	public Card(String rank, int suit, int value, Image img) {
		this.rank = rank;
		this.suit = suit;
		this. value = value;
		this.img = img;
		width = img.getWidth(null);
		height = img.getHeight(null);
	}//end constructor
	
	/*****Getters*****/
	public String getRank() {
		return rank;
	}
	
	public int getSuit() {
		return suit;
	}
	
	public String getSuitName() {
		if (value == 0) {
			return "Hearts";
		} else if (value == 1) {
			return "Diamonds";
		} else if (value == 2) {
			return "Spades";
		} else {
			return "Clubs";
		}
	}
	
	public int getValue() {
		return value;
	}
	
	public Image getImage() {
		return img;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setXY(int x, int y) {
		this.x = x;
		this.y = y;
	}//end setXY
	
	public void addToXY(int changeX, int changeY) {
		//add to x y values
		x += changeX;
		y += changeY;
	}//end addToXY
	
	public void draw(Graphics g) {
		g.drawImage(img, x, y, null);
		Card.drawOutline(g, x, y);
	}//end draw
	
	public static void drawOutline(Graphics g, int x, int y) {
		g.setColor(Color.BLACK);
		g.drawRoundRect(x, y, width, height, 8, 8);
	}//end drawOutline()
	
	
}
