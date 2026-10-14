
(() => {
 const root=document.querySelector('[data-management]'); if(!root)return;
 const rows=[...root.querySelectorAll('[data-row]')], search=root.querySelector('[data-search]'), buttons=[...root.querySelectorAll('[data-filter]')];
 let active='';
 const normalize=value=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('es').trim();
 function refresh(){let visible=0;const query=normalize(search.value);rows.forEach(row=>{row.hidden=!(normalize(row.dataset.name).includes(query)&&(!active||row.dataset.category===active));if(!row.hidden)visible++;});root.querySelector('[data-count]').textContent=`${visible} de ${rows.length}`;const empty=root.querySelector('[data-empty]');empty.hidden=visible>0;if(rows.length)empty.textContent='No hay coincidencias. Probá otro nombre o cambiá el filtro.';}
 buttons.forEach(button=>{const count=rows.filter(row=>!button.dataset.filter||row.dataset.category===button.dataset.filter).length;button.querySelector('b').textContent=count;if(button.dataset.filter==='none'&&!count)button.hidden=true;button.addEventListener('click',()=>{active=button.dataset.filter;buttons.forEach(b=>b.setAttribute('aria-pressed',String(b===button)));refresh();});});
 root.querySelector('.management-tools').hidden=false;search.addEventListener('input',refresh);refresh();
 const panel=root.querySelector('[data-create-panel]'), open=root.querySelector('[data-open-create]'), close=panel.querySelector('[data-close-create]');
 if(typeof HTMLDialogElement!=='undefined'&&HTMLDialogElement.prototype.showModal){const dialog=document.createElement('dialog');dialog.className='management-dialog';dialog.setAttribute('aria-labelledby','create-title');panel.before(dialog);dialog.append(panel);open.hidden=false;close.hidden=false;open.addEventListener('click',()=>dialog.showModal());close.addEventListener('click',()=>dialog.close());dialog.addEventListener('close',()=>open.focus());}
 const type=panel.querySelector('[data-task-type]'), area=panel.querySelector('[data-task-area]');
 if(type&&area){const container=panel.querySelector('#areaSelectDiv');function update(){const specific=type.value==='SPECIFIC';container.style.display=specific?'block':'none';area.required=specific;area.disabled=!specific;}type.addEventListener('change',update);update();}
})();
