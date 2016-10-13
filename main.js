var isLE = true;
var data = void(0);
var data_edited = void(0);
var file = void(0);
var fileBuf = void(0);
var reader = new FileReader();
$(document.body).append('<input type="file" id="file_input" style="display:none;">');

$( function() {
  if(!isCompatible()) alert("お使いのブラウザは対応していません。\n推奨ブラウザはFireFox、Chromeの最新バージョンです。\n\n正常に使用できずセーブデータが破損する可能性があります。対応ブラウザにてご使用ください。")
  $('input').val('');

  $('#file_input').change(function(){ // ファイルが選択された時
    data = {};
    data_edited = {};
    file = this.files[0];
    $('#file_name').html(file.name);
    reader.readAsArrayBuffer(file);
  });

  $('[data-edit]').change(function(){ // ファイルが選択された時
    if(data === void(0)) return;
    var target = $(this).data('edit');
    data_edited[target] = $(this).val();
  });

  $('#button_exp_max').click(function(){
    if(data === void(0)) return;
    $('#value_lv').val(99);
    $('#value_exp').val(25165822);
    $('#value_lv, #value_exp').change();
  });
});

reader.onload = function(evt){
  fileBuf = evt.target.result; //ファイル内容を全て取得

	var dataView = new DataView( fileBuf );

  // 名前を抽出（MHX）
  /*var nameBuf = fileBuf.slice(0xc7a1c, 0xc7a1c+32);
  window.example = nameBuf;
  data.name = new TextDecoder().decode(nameBuf).replace(/\x00/g, "");
  $('#value_name').val(data.name);*/

  // 所持金を抽出
	data.zenny = dataView.getUint32(0x5B404, isLE);
  $('#value_zenny').val(data.zenny);

  data.lv = dataView.getUint8(0x9E64, isLE);
  $('#value_lv').val(data.lv);

  data.exp = dataView.getUint32(0x9E68, isLE);
  $('#value_exp').val(data.exp);

  // アイテム抽出（変数セット→ループ）
  var count = 1;
  data.item = {};
  data_edited.item = {};
  data.item_pos = {};

  for(var pos=0; pos<=1496; pos++) {
    var addr = 0x10 + 8*pos;
    var id = dataView.getUint16( addr , isLE );
    var idStr = ( '000' + id.toString(16).toUpperCase() ).slice(-4);
    var name = itemList[id];
    data.item[id] = dataView.getUint16( addr + 2 , isLE );
    if(data.item[id] === 0) continue;
    data.item_pos[id] = pos;
    if(name === void(0)) name = '？？？？？？';
    var tr = $('<tr></tr>').appendTo('table#list_item > tbody');
    $('<th scope="row"></th>').html( count ).appendTo(tr);
    var td_addr = $('<td></td>').html( '0x' + ( '000' + addr.toString(16).toUpperCase() ).slice(-4) ).appendTo(tr);
    var td_pos = $('<td></td>').html( (pos+1) + '番目' ).appendTo(tr);
    var td_id = $('<td></td>').html( idStr ).appendTo(tr);
    var td_name = $('<td></td>').html( name ).appendTo(tr);
    var td_num = $('<td></td>').html( data.item[id] ).data('item_id', id).appendTo(tr);

    td_num.click(function(){
      var id = $(this).data('item_id');
      var value = prompt('新しい値を入力してください', data.item[id]);
      if(value === null) return;
      value = parseInt(value);
      if( !(0 < value && value < 1000) ) {alert('正常な値（1-999個）を入力してください'); return}

      data_edited.item[id] = value;
      $(this).html(value);
    });
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

  if('zenny' in data_edited) dataView_new.setUint32(0x5B404, parseInt(data_edited.zenny), isLE); // 所持金の場合
  if('lv' in data_edited) dataView_new.setUint8(0x9E64, parseInt(data_edited.lv), isLE); // レベルの場合
  if('exp' in data_edited) dataView_new.setUint32(0x9E68, parseInt(data_edited.exp), isLE); // 経験値の場合

  $.each(data_edited.item, function(id, val){ // アイテムの場合
    var pos = data.item_pos[id];
    var addr = 0x10 + 8*(pos) + 2;
    alert(data.item[id] !== val);
    if(data.item[id] !== val) dataView_new.setUint16(addr, val, isLE);
  });
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
