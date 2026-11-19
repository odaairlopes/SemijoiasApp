(function () {
  var ATTR = 'data-wpp';
  var BASE = 'https://lojatoquedebeleza.semijoias.net/?code=';
  var SEND_LABEL = '💬 Compartilhar no WhatsApp';
  var COPY_MSG_LABEL = '📋 Copiar mensagem';
  var COPY_LINK_LABEL = '🔗 Copiar link do produto';

  // Prefixo do código do produto -> emoji da categoria
  var EMOJI = {
    AN: '💍', BR: '👂', BF: '👂',
    CL: '📿', COR: '⛓️', CHO: '📿',
    CON: '💎', GFC: '💎', PUL: '✨',
    PU: '✨', TOR: '🦶'
  };
  var DEFAULT_EMOJI = '💍';

  function emoji(cod) {
    var upper = (cod || '').toUpperCase();
    var keys = Object.keys(EMOJI);
    for (var i = 0; i < keys.length; i++) {
      if (upper.indexOf(keys[i]) === 0) return EMOJI[keys[i]];
    }
    return DEFAULT_EMOJI;
  }

  function liText(card, label) {
    var items = card.querySelectorAll('ul li');
    for (var i = 0; i < items.length; i++) {
      var b = items[i].querySelector('b');
      if (b && b.textContent.trim() === label) {
        var clone = items[i].cloneNode(true);
        var noise = clone.querySelectorAll('img,p,b');
        for (var j = 0; j < noise.length; j++) noise[j].remove();
        return clone.textContent.trim().replace(/\s+/g, ' ') || null;
      }
    }
    return null;
  }

  function parseCard(card) {
    var h5 = card.querySelector('h5');
    var codEl = h5 && h5.querySelector('label');
    var cod = codEl ? codEl.textContent.trim() : '';
    var match = h5 ? h5.textContent.match(/\(([^)]+)\)/) : null;
    var imgEl = card.querySelector('img[src]');
    return {
      cod: cod,
      nome: match ? match[1].trim() : '',
      emoji: emoji(cod),
      valor: liText(card, 'Valor:'),
      desc: liText(card, 'Descrição:'),
      tam: liText(card, 'Tamanho:'),
      img: imgEl ? imgEl.src : '',
      link: cod ? BASE + encodeURIComponent(cod) : ''
    };
  }

  function buildMsg(p) {
    var m = p.emoji + ' *' + p.cod + '* - ' + p.nome + '\n\n';
    if (p.desc) m += '📋 *Descrição:*\n' + p.desc + '\n\n';
    if (p.tam) m += '📏 *Tamanho:* ' + p.tam + '\n\n';
    if (p.valor) m += '💰 *Valor:* ' + p.valor + '\n\n';
    if (p.link) m += '🔗 *Ver produto:*\n' + p.link;
    return m.trim();
  }

  function injectStyles() {
    if (document.getElementById('_wcss')) return;
    var s = document.createElement('style');
    s.id = '_wcss';
    s.textContent =
      '._wg{margin-top:6px;padding-top:8px;border-top:1px solid #eee;display:flex;flex-direction:column;gap:4px;background:transparent}' +
      '._wb{display:flex;align-items:center;justify-content:center;gap:6px;padding:12px 14px;border-radius:8px;font-size:14px;font-weight:700;cursor:pointer;border:none;width:100%;font-family:inherit;text-decoration:none;box-sizing:border-box}' +
      '._wbg{background:#25D366;color:#fff;margin-bottom:4px}' +
      '._wbo{background:#fff;color:#444;border:1px solid #ccc;margin-bottom:4px}' +
      '._wck{position:absolute;top:8px;right:8px;width:24px;height:24px;accent-color:#25D366;cursor:pointer}' +
      '.card{position:relative!important}' +
      '._wfb{position:fixed;bottom:0;left:0;right:0;padding:16px;background:#128C7E;color:#fff;border:none;font-size:15px;font-weight:700;cursor:pointer;z-index:99999;display:none;font-family:inherit;box-shadow:0 -2px 12px rgba(0,0,0,.2)}' +
      '._wfb.on{display:block}';
    document.head.appendChild(s);
  }

  // Botão flutuante: envia em lote os produtos marcados (somente texto)
  function ensureBatchBar() {
    var bar = document.getElementById('_wbar');
    if (bar) return bar;
    bar = document.createElement('button');
    bar.id = '_wbar';
    bar.className = '_wfb';
    bar.addEventListener('click', function () {
      var checked = document.querySelectorAll('._wck:checked');
      var msgs = [];
      for (var i = 0; i < checked.length; i++) {
        var card = checked[i].closest('.card');
        if (card) msgs.push(buildMsg(parseCard(card)));
      }
      if (!msgs.length) return;
      var separator = '\n\n──────────\n\n';
      window.open('https://api.whatsapp.com/send?text=' + encodeURIComponent(msgs.join(separator)), '_blank');
    });
    document.body.appendChild(bar);
    return bar;
  }

  var bar = ensureBatchBar();

  function updateBar() {
    var n = document.querySelectorAll('._wck:checked').length;
    bar.textContent = '📤 Enviar ' + n + ' produto' + (n !== 1 ? 's' : '') + ' no WhatsApp';
    bar.className = '_wfb' + (n > 0 ? ' on' : '');
  }

  function makeButton(className, label) {
    var btn = document.createElement('button');
    btn.className = className;
    btn.textContent = label;
    return btn;
  }

  function copyButton(label, doneLabel, text) {
    var btn = makeButton('_wb _wbo', label);
    btn.addEventListener('click', function () {
      navigator.clipboard.writeText(text).then(function () {
        btn.textContent = doneLabel;
        setTimeout(function () { btn.textContent = label; }, 2000);
      });
    });
    return btn;
  }

  function injectCard(card) {
    if (card.hasAttribute(ATTR)) return;
    card.setAttribute(ATTR, '1');

    var p = parseCard(card);
    var msg = buildMsg(p);

    var chk = document.createElement('input');
    chk.type = 'checkbox';
    chk.className = '_wck';
    chk.addEventListener('change', updateBar);
    card.appendChild(chk);

    var group = document.createElement('li');
    group.className = '_wg';

    // Compartilha foto + texto via ponte nativa; sem ela, cai para o link de texto
    var share = makeButton('_wb _wbg', SEND_LABEL);
    share.addEventListener('click', function () {
      share.textContent = '⏳ Preparando imagem...';
      if (window.AndroidShare && p.img) {
        window.AndroidShare.shareImageToWhatsApp(p.img, msg);
      } else {
        window.open('https://api.whatsapp.com/send?text=' + encodeURIComponent(msg + '\n\n' + p.img), '_blank');
      }
      setTimeout(function () { share.textContent = SEND_LABEL; }, 3000);
    });
    group.appendChild(share);

    group.appendChild(copyButton(COPY_MSG_LABEL, '✅ Copiado!', msg));
    if (p.link) group.appendChild(copyButton(COPY_LINK_LABEL, '✅ Link copiado!', p.link));

    var ul = card.querySelector('ul');
    if (ul) ul.appendChild(group);
  }

  function injectAll() {
    var cards = document.querySelectorAll('#ProductsList .card');
    for (var i = 0; i < cards.length; i++) injectCard(cards[i]);
  }

  injectStyles();
  injectAll();

  // A listagem é paginada via AJAX: reinjeta quando o DOM mudar
  var target = document.getElementById('product-pages') || document.body;
  new MutationObserver(function () { setTimeout(injectAll, 200); })
    .observe(target, { childList: true, subtree: true });
})();
