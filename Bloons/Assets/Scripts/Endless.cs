using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Endless : MonoBehaviour {

    public int numBalloonsTotal = 50;
    public int numMade = 0;
    public GameObject blueBalloonPrefab;
    public GameObject redBalloonPrefab;
    public GameObject greenBalloonPrefab;
    public Vector3 balloonPosMin = new Vector3(-15, -8, 0);
    public Vector3 balloonPosMax = new Vector3(10, 5, 0);
   
    protected SceneController SC;
    protected Bow B;
    private GameObject[] balloonInstances;

    private void Awake()
    {
       // B.ResetScore();
    }
    // Use this for initialization
    void Start () {
        SC = FindObjectOfType<SceneController>();
       B = FindObjectOfType<Bow>();
	}
	
	// Update is called once per frame
	void Update () {

        GameObject crafter = GameObject.Find("BalloonCrafter");
        GameObject balloon;

        while (numMade < numBalloonsTotal)
        {
            numMade++;
            int rand = Random.Range(0, 3);
            if (rand == 0)
            {
                balloon = Instantiate<GameObject>(blueBalloonPrefab);
            }
            else if (rand == 1)
            {
                balloon = Instantiate<GameObject>(redBalloonPrefab);
            }
            else
            {
                balloon = Instantiate<GameObject>(greenBalloonPrefab);
            }

            Vector3 bPos = Vector3.zero;
            bPos.x = Random.Range(balloonPosMin.x, balloonPosMax.x);
            bPos.y = Random.Range(balloonPosMin.y, balloonPosMax.y);

            bPos.z = 0;

            balloon.transform.position = bPos;
            balloon.transform.SetParent(crafter.transform);
            SC.BalloonMade();
        } //end for
       // numPopped = 0;
    }

    public void BalloonPopped() {
        numMade--;
    }
}
