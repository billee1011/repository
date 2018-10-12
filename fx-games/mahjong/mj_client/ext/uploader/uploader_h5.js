var mime = {'png': 'image/png', 'jpg': 'image/jpeg', 'jpeg': 'image/jpeg', 'bmp': 'image/bmp'};
var selectedHandler;
var thisRef;
function selectImage(selectedFunc,thisValue) {
    selectedHandler = selectedFunc;
    thisRef = thisValue;
    var fileInput = document.getElementById("fileInput");
    if(fileInput==null){
        fileInput = document.createElement("input");
        fileInput.id = "fileInput";
        fileInput.type = "file";
        fileInput.accept = "image/*";
        fileInput.style.height = "0px";
        fileInput.style.display = "block";
        fileInput.style.overflow = "hidden";
        document.body.insertBefore(fileInput,document.body.firstChild);
        fileInput.addEventListener('change', tmpSelectFile, false);
    }
    fileInput.click();
}
function tmpSelectFile(evt) {
    var file = evt.target.files[0];
    var type = file.type;
    if (!type) {
        type = mime[file.name.match(/\.([^\.]+)$/i)[1]];
    }
    var reader = new FileReader();
    function tmpLoad() {
        var re = /^data:base64,/;
        var ret = this.result + '';
        if (re.test(ret)) ret = ret.replace(re, 'data:' + mime[type] + ';base64,');
        tmpCreateImage && tmpCreateImage(ret);
    }
    reader.onload = tmpLoad;
    reader.readAsDataURL(file);
}
function tmpCreateImage(uri) {
    selectedHandler & selectedHandler(thisRef,uri);
}