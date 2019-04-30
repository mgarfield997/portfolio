using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Balloon : MonoBehaviour {

    private GameObject ball;
    private Endless E;
    private SceneController SC;
    private Bow B;
	// Use this for initialization
	void Start () {
        B = FindObjectOfType<Bow>();
        ball = this.gameObject;
        E = FindObjectOfType<Endless>();
        SC = FindObjectOfType<SceneController>();
	}
	
	// Update is called once per frame
	void Update () {
		
	}

    private void OnTriggerEnter(Collider other)
    {
        if (other.gameObject.tag == "projectile") {
            if (ball.name.Contains("Blue"))
            {
                SC.AddToScore(1);
            } else if (ball.name.Contains("Green")) {
                SC.AddToScore(2);
            } else if (ball.name.Contains("Red")) {
                SC.AddToScore(3);
            }

            if (E != null) {
                E.BalloonPopped();
            }

            SC.BalloonPopped();

            Destroy(this.gameObject);
        } // end if

    } //end onTriggerEnter
}
