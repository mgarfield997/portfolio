using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class Bow : MonoBehaviour {
    /** Author: Megan Garfield
 * Date Created: 12/12/18
 * Last Updated: 12/12/18
 **/

    static private Bow B;

    private static int score = 0;

    [Header("Set In Inspector")]
    public Text scoreText;
    public GameObject prefabProjectile;
    public float velocityMult = 13f;

    //fields set dynamically
    [Header("Set dynamically")]
    public GameObject launchPoint;
    public Vector3 launchPos;
    public GameObject projectile;
    public bool aimingMode;
    private Rigidbody projectileRigidBody;

    static public Vector3 LAUNCH_POS
    {
        get
        {
            if (B == null)
            {
                return Vector3.zero;
            }
            return B.launchPos;
        }
    }

    private void Awake()
    {
        B = this;
        Transform launchPointTrans = transform.Find("LaunchPoint");
        launchPoint = launchPointTrans.gameObject;
        launchPoint.SetActive(false);
        launchPos = launchPointTrans.position;
      //  SetScoreText();
    } //end awake

    // Use this for initialization
    void Start () {
		
	}
	
	// Update is called once per frame
	void Update () {
        if (!aimingMode) return;

        Vector3 mousePos2D = Input.mousePosition;
        mousePos2D.z = -Camera.main.transform.position.z;
        Vector3 mousePos3D = Camera.main.ScreenToWorldPoint(mousePos2D);

        Vector3 mouseDelta = mousePos3D - launchPos;
        float maxMagnitude = this.GetComponent<SphereCollider>().radius;
        if (mouseDelta.magnitude >= maxMagnitude)
        {
            mouseDelta.Normalize();
            mouseDelta *= maxMagnitude;
        } //end if

        //Move Projectile
        Vector3 projPos = launchPos + mouseDelta;
        projectile.transform.position = projPos;

        if (Input.GetMouseButtonUp(0))
        {
            aimingMode = false;
            projectileRigidBody.isKinematic = false;
            projectileRigidBody.velocity = -mouseDelta * velocityMult;
            projectile = null;
        }
	} //end update

    private void OnMouseEnter()
    {
        launchPoint.SetActive(true);
    } //end mouseEnter

    private void OnMouseExit()
    {
        launchPoint.SetActive(false);
    } //end mouseExit

    private void OnMouseDown()
    {
      //  AddToScore(-2);
        aimingMode = true;
        projectile = Instantiate(prefabProjectile) as GameObject;
        projectile.transform.position = launchPos;

        projectileRigidBody = projectile.GetComponent<Rigidbody>();
        projectileRigidBody.isKinematic = true;
    } //end mouseDown

    public void AddToScore(int points) {
        score += points;
      //  SetScoreText();
    }

    private void SetScoreText() {
        scoreText.text = "Score: " + score.ToString();
    }

    public void ResetScore() {
        score = 0;
    } 


}
