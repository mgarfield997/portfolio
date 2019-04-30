using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

public class SceneController : MonoBehaviour {

    [Header("Set In Inspector")]
    public int numBalloons;
    public Text scoreText;
    public Text endText;

    private static int score = 0;
    // Use this for initialization
    private void Awake()
    {
        SetScoreText();
        SetEndText();
    }
    void Start () {
		
	}
	
	// Update is called once per frame
	void Update () {
        if (Input.GetKey(KeyCode.Escape)) {
            Application.Quit();
        }

        if (numBalloons == 0) {
            LoadScene(6);
        }
	}

    public void LoadScene(int scene) {
        if (scene < 6) {
            score = 0; 
        }
        SceneManager.LoadScene(scene);
    }

    public void BalloonPopped() {
        numBalloons--;
    }

    public void BalloonMade(){
        numBalloons++;
    }

    public void AddToScore(int points)
    {
        score += points;
        SetScoreText();
    }

    private void SetScoreText()
    {
        scoreText.text = "Score: " + score.ToString();
    }

    private void SetEndText() {
        /*  endText.text = "So you decided to cut your losses and end the game. " +
              "You may have possibly gotten a better score than if you had continued, " +
              "but you didn't get the satisfaction of popping all the balloons, " +
              "so did you really win? \n Final Score: " + score.ToString() +
              "\n\nWould you like to pop them all or try a different mode ?";
      */
        endText.text = "Final Score: " + score.ToString();
}

    public void ResetScore()
    {
        score = 0;
    }

    public void EndGame() {
        Application.Quit();
    }
}
