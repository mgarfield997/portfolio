package blackjack;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import gameName.Deck;

public class DrawDeckImages {
/**
* @author mlg932
* Date Created: 8/28/2018
* Last Updated: 9/11/2018
*/
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		//Get Deck Values
		String[] suits = Deck.getSuitSymbols();
		String[] ranks = Deck.getRanks();
		int cardWidth = Deck.getCardWidth();
		int cardHeight = Deck.getCardHeight();
		
		//Screen Size
		int imageWidth = 13*cardWidth;
		int imageHeight = 4*cardHeight;
		
		BufferedImage img = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		
		//Background
		Graphics g = img.getGraphics();
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(new Color(0,0,0,0));
		g2.fillRect(0, 0, imageWidth, imageHeight);
		
		//Font Style
		Font font = new Font(Font.DIALOG, Font.BOLD, 24);
		g2.setFont(font);
		FontMetrics fm = g.getFontMetrics(font);
		
		//Draw Cards
		for (int row = 0, y = 0; row < 4; row++, y += cardHeight) {
			for(int col = 0, x = 0; col < 13; col++, x +=cardWidth) {
				g2.setColor(Color.WHITE);
				g2.fillRoundRect(x, y, cardWidth-1, cardHeight-1, 8, 8);
				
				//check row color
				if (row < 2) {
					g2.setColor(Color.RED);
				} else {
					g2.setColor(Color.BLACK);
				}
				
				String rank = ranks[col];
				String suit = suits[row];
				int rankWidth = fm.stringWidth(rank);
				int suitWidth = fm.stringWidth(suit);
				int rankLeft = x + cardWidth/2 - rankWidth/2;
				int rankTop = y + 20;
				int suitLeft = x + cardWidth/2 - suitWidth/2;
				int suitTop = rankTop + 22;
				g2.drawString(rank, rankLeft, rankTop);
				
				g2.drawString(suit, suitLeft, suitTop);
				
			} //for cols
		} //for rows
				
		String fileName = "cards.png";
		File file = new File(fileName);
		
		try {
			ImageIO.write(img, "png", file);
		} catch(IOException e) {
			String message = "Could not save" + fileName;
			JOptionPane.showMessageDialog(null, message);
		}
		
	}
	//end main

}
//end class
