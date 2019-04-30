using System.Collections;
using System.Collections.Generic;
using UnityEngine;
/** Author: Megan Garfield
 * Date Created: 12/12/18
 * Last Updated: 12/12/18
 **/

public class BalloonGenerator : MonoBehaviour {

    public int numBalloons = 50;
    public GameObject blueBalloonPrefab;
    public GameObject redBalloonPrefab;
    public GameObject greenBalloonPrefab;
    public Vector3 balloonPosMin = new Vector3(-15, -8, 0);
    public Vector3 balloonPosMax = new Vector3(10, 5, 0);

    private GameObject[] balloonInstances;
    private Bow B;
    private SceneController SC;

    private void Awake()
    {

        balloonInstances = new GameObject[numBalloons];
        GameObject crafter = GameObject.Find("BalloonCrafter");
        GameObject balloon;

        for (int i = 0; i < numBalloons; i++)
        {
            if (i % 3 == 0)
            {
                balloon = Instantiate<GameObject>(blueBalloonPrefab);
            } else if (i % 3 == 1) {
                balloon = Instantiate<GameObject>(redBalloonPrefab);
            } else {
                balloon = Instantiate<GameObject>(greenBalloonPrefab);
            }

            Vector3 bPos = Vector3.zero;
            bPos.x = Random.Range(balloonPosMin.x, balloonPosMax.x);
            bPos.y = Random.Range(balloonPosMin.y, balloonPosMax.y);

            bPos.z = 0;

            balloon.transform.position = bPos;
            balloon.transform.SetParent(crafter.transform);
            balloonInstances[i] = balloon;
        }
    }

    // Use this for initialization
    void Start () {
        B = FindObjectOfType<Bow>();
        SC = FindObjectOfType<SceneController>();

    }

    // Update is called once per frame
    void Update () {
		
	}
}
