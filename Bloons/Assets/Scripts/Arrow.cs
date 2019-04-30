using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Arrow : MonoBehaviour {
    /**
     * Author: Megan Garfield
     * Date Created: 11/28/18
     * Last Modified: 11/15/18
     **/

	// Use this for initialization
	void Start () {
		
	}
	
	// Update is called once per frame
	void Update () {
        var mousePos = Input.mousePosition;
        mousePos.z = 10.0f; //The distance from the camera to the player object
        Vector3 lookPos = Camera.main.ScreenToWorldPoint(mousePos);
        lookPos = lookPos - transform.position;
        float angle = Mathf.Atan2(lookPos.x, lookPos.y) * Mathf.Rad2Deg;
        transform.rotation = Quaternion.AngleAxis(angle, new Vector3(0,1,1));

        if ((transform.position.x > 15) || (transform.position.x < -25)
            || (transform.position.y > 10) || (transform.position.y < -10)) {
            Destroy(this.gameObject);
        }
	}

    private void Awake()
    {
        
    }

}
