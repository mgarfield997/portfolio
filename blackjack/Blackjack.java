package blackjack;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.UIManager;

public class Blackjack extends JFrame {
/**
 * Author: Megan Garfield
 * Date Created: 9/11/2018
 * Last Updated: 9/11/2018
 */
	
	//Serialization of recreation
	private static final long serialVersionUID = 1L;
	
	//create table panel
	private TablePanel tablePanel = new TablePanel();
	
	//constructor
	public Blackjack() {
		initGUI();
		
		setTitle("Blackjack"); //window title
		setResizable(false); //can't resize window
		pack(); //pack the window
		setLocationRelativeTo(null); //center window
		setVisible(true); //make window visible
		setDefaultCloseOperation(EXIT_ON_CLOSE); //exit on close
	} 
	//end constructor
	
	//Initialize GUI - creates GUI
	private void initGUI() {
		TitleLabel gameTitle = new TitleLabel("Blackjack");
		add(gameTitle, BorderLayout.PAGE_START);
		add(tablePanel, BorderLayout.CENTER);
	} 
	//end initGUI
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			String className = UIManager.getCrossPlatformLookAndFeelClassName();
			UIManager.setLookAndFeel(className);
		} catch (Exception e){}
			
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				new Blackjack();
			} //end run
		} //end Runnable
		); //end invokeLater
	} 
	//end main

}