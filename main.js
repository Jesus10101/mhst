var isLE = true;
var file = void(0);
var fileBuf = void(0);
var data = {};
var reader = new FileReader();
$(document.body).append('<input type="file" id="file_input" style="display:none;">');

$( function() {
  if(!isCompatible()) alert("お使いのブラウザは対応していません。\n推奨ブラウザはFireFox、Chromeの最新バージョンです。\n\n正常に使用できずセーブデータが破損する可能性があります。対応ブラウザにてご使用ください。")
  $('input').val('');

  $('#file_input').change(function(){ // ファイルが選択された時
    file = this.files[0];
    $('#file_name').html(file.name);
    reader.readAsArrayBuffer(file);
  });
});

reader.onload = function(evt){
  fileBuf = evt.target.result; //ファイル内容を全て取得

	var dataView = new DataView( fileBuf );

  // 所持金を抽出
	data.zenny = dataView.getUint32(0x5B404, isLE);
  $('#value_zenny').val(data.zenny);

  // 名前を抽出（MHX）
  /*var nameBuf = fileBuf.slice(0xc7a1c, 0xc7a1c+32);
  window.example = nameBuf;
  data.name = new TextDecoder().decode(nameBuf).replace(/\x00/g, "");
  $('#value_name').val(data.name);*/

  var count = 1;
  data.item = [];
  for(var i=0; i<=1496; i++) {
    var n = i+1;
    var addr = 0x10 + 8*(n-1);
    var id = dataView.getUint16( addr , isLE );
    var idStr = ( '000' + id.toString(16).toUpperCase() ).slice(-4);
    var name = itemList[id];
    data.item[id] = dataView.getUint16( addr + 2 , isLE );
    if(data.item[id] === 0) continue;
    if(name === void(0)) name = '？？？？？？';
    var tr = $('<tr></tr>').appendTo('table#list_item > tbody');
    $('<th scope="row"></th>').html( count ).appendTo(tr);
    var td_addr = $('<td></td>').html( '0x' + ( '000' + addr.toString(16).toUpperCase() ).slice(-4) ).appendTo(tr);
    var td_pos = $('<td></td>').html( (n-1) + '番目' ).appendTo(tr);
    var td_id = $('<td></td>').html( idStr ).appendTo(tr);
    var td_name = $('<td></td>').html( name ).appendTo(tr);
    var td_num = $('<td></td>').html( data.item[id] ).appendTo(tr);
    count++;
  }
}

function saveFile() {
  var fileBuf_new = fileBuf.slice(0);
  // 編集前と値を比較
  /*if(data.name !== $('#value_name').val()) { // 名前の場合（MHX）
    /*var newNameBuf = new TextEncoder().encode(value);
    fillInNull(buffer, 0xC7A1C, 32)
    buffer.merge(newNameBuf, 0xC7A1C);
  }*/
  var dataView_new = new DataView(fileBuf_new);
  if(data.zenny != $('#value_zenny').val()) dataView_new.setUint32(0x5B404, parseInt($('#value_zenny').val(), 10), isLE); // 所持金の場合
  makeBlob('mhr_game0.sav', fileBuf_new);
}

function makeBlob(fileName, data) {
  var now = new Date();
  var blob = new Blob([data], {type: "application/octet-binary"});
  if(fileName){
      if (navigator.msSaveBlob) {
          // IE用
          navigator.msSaveBlob(blob, fileName);
      } else {
          var aelm = $("<a>").attr({
              href: URL.createObjectURL(blob),
              download: fileName
          }).text(fileName)[0];
          var evt = document.createEvent("MouseEvents");
          evt.initEvent("click", false, true);
          aelm.dispatchEvent(evt);
      }
  }
}

function writeValue(buffer, name, value) {
  switch (name) {
    case 'name':
      break;
    case "zenny": // 所持金の場合
    default:

  }
}

function fillInNull(buffer, pos, bytes) {
  var nullBuf = new ArrayBuffer(bytes);
  buffer.merge(nullBuf, pos);
}

function isCompatible() {
  return window.ArrayBuffer
      && window.TextDecoder
      ;
}

ArrayBuffer.prototype.merge = function(that, pos){
  if(pos == void(0)) pos = 0;
  var whole = new Uint8Array(this);
  whole.set(new Uint8Array(that), pos);
  return whole.buffer;
}
