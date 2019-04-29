package blackjack;

import java.awt.Font;
import java.awt.Color;

import javax.swing.JLabel;

public class TitleLabel extends JLabel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @author Megan Garfield
	 * Date Created: 8/28/2018
	 * Last Updated: 9/11/2018
	 */
	
	//Constructor
	public TitleLabel(String title) { 
		Font myFont = new Font(Font.SERIF, Font.BOLD, 32);
		setFont(myFont);
		
		setBackground(Color.BLACK);
		setForeground(Color.WHITE);
		setOpaque(true);
		setHorizontalAlignment(JLabel.CENTER);
		setText(title);
	} 
	//End Constructor
}

