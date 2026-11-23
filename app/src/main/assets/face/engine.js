'use strict';
// The original pipeline, without descriptor normalization or a replacement model.
// No camera access, network upload, persistence or session credentials in this page.
window.FaceEngine = (() => {
  let ready = false;
  const send = value => FaceNative.result(JSON.stringify(value));
  const distance = (a,b) => Math.hypot(a.x-b.x,a.y-b.y);
  const ear = eye => (distance(eye[1],eye[5])+distance(eye[2],eye[4])) /
      Math.max(0.001, 2*distance(eye[0],eye[3]));
  const center = points => points.reduce((a,p)=>({x:a.x+p.x/points.length,y:a.y+p.y/points.length}),{x:0,y:0});
  async function init() {
    try {
      const started=performance.now();
      const load = async (name, net) => {
        const begin=performance.now();
        await net.loadFromUri('models');
        return [name, performance.now()-begin];
      };
      const stages=await Promise.all([
        load('detector',faceapi.nets.ssdMobilenetv1),
        load('landmarks',faceapi.nets.faceLandmark68Net),
        load('descriptor',faceapi.nets.faceRecognitionNet)
      ]);
      ready=true;
      const resources=performance.getEntriesByType('resource');
      const modelResources=resources.filter(entry => entry.name.includes('/face/models/'));
      const scriptResources=resources.filter(entry => entry.name.endsWith('/face-api.min.js'));
      send({kind:'ready',backend:faceapi.tf.getBackend(),
        page_to_init_ms:started,load_ms:performance.now()-started,
        detector_load_ms:stages[0][1],landmarks_load_ms:stages[1][1],descriptor_load_ms:stages[2][1],
        model_resource_ms:modelResources.length ? Math.max(...modelResources.map(e=>e.responseEnd))-
            Math.min(...modelResources.map(e=>e.startTime)) : -1,
        script_resource_ms:scriptResources.length ? scriptResources[0].duration : -1});
    } catch(e) { send({kind:'error',code:'MODEL_LOAD_FAILED'}); }
  }
  async function process(id, data) {
    if(!ready) {send({id,kind:'error',code:'NOT_READY'}); return;}
    const started=performance.now();
    let img;
    try {
      img = new Image();
      await new Promise((resolve,reject)=>{img.onload=resolve;img.onerror=reject;img.src=data;});
      const decoded=performance.now();
      const faces=await faceapi.detectAllFaces(img,new faceapi.SsdMobilenetv1Options({minConfidence:0.5}))
          .withFaceLandmarks().withFaceDescriptors();
      const inferred=performance.now();
      const timings={ms:inferred-started,decode_ms:decoded-started,network_ms:inferred-decoded};
      if(faces.length!==1) {send({id,kind:'frame',count:faces.length,...timings});return;}
      const f=faces[0], l=f.landmarks, left=center(l.getLeftEye()),right=center(l.getRightEye());
      const mid={x:(left.x+right.x)/2,y:(left.y+right.y)/2};
      const nose=l.getNose()[3], interocular=Math.max(1,distance(left,right));
      const descriptor=Array.from(f.descriptor);
      if(descriptor.length!==128 || !descriptor.every(Number.isFinite)) throw Error('VECTOR');
      send({id,kind:'frame',count:1,descriptor,landmarks:l.positions.map(p=>[p.x/img.width,p.y/img.height]),ear:(ear(l.getLeftEye())+ear(l.getRightEye()))/2,
        yaw:(nose.x-mid.x)/interocular,score:f.detection.score,
        faceRatio:f.detection.box.width/img.width,
        box:[f.detection.box.x/img.width,f.detection.box.y/img.height,f.detection.box.width/img.width,f.detection.box.height/img.height],
        ...timings});
    } catch(e) {send({id,kind:'error',code:'INFERENCE_FAILED'});}
    finally {if(img)img.src='';}
  }
  return {init,process};
})();
FaceEngine.init();
