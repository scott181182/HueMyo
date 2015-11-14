package com.bixfordstudios.hue;

public class Main
{
	private MainFrame frame;
	private MainController controller;
	
	public static void main(String[] args)
	{
		@SuppressWarnings("unused")
		Main main = new Main();
	}
	public Main()
	{
        controller = new MainController();
        frame = new MainFrame(controller);
        controller.setFrame(frame);
	}
	
}
