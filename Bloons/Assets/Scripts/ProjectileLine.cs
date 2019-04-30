using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ProjectileLine : MonoBehaviour {

    static public ProjectileLine S;

    [Header("Set in Inspector")]
    public float minDist = 0.1f;

    private LineRenderer line;
    private List<Vector3> points;

    private void Awake()
    {
        S = this;
        line = GetComponent<LineRenderer>();
        line.enabled = false;
        points = new List<Vector3>();
    }
    // Use this for initialization
    void Start () {
		
	}
	
	// Update is called once per frame
	void Update () {
		
	}

    public void Clear(){
        line.enabled = false;
        points = new List<Vector3>();
    }

    public void AddPoints() {
       // if (points.Count > 0) {
        //    return;
        //}

        if (points.Count == 0) {
            // Vector3 launchPosDiff = 
            line.positionCount = 2;
            line.SetPosition(0, points[0]);
            line.SetPosition(1, points[1]);
            line.enabled = true;
        } else {
            line.positionCount = points.Count;
            line.SetPosition(points.Count - 1, lastPoint);
            line.enabled = true;
        } //end else

    } //end addpoints

    public Vector3 lastPoint {
        get {
            if (points == null) {
                return (Vector3.zero);
            } else {
                return (points[points.Count - 1]);
            }
        }
    } //end last point

    private void FixedUpdate()
    {
        AddPoints();
    }
}
