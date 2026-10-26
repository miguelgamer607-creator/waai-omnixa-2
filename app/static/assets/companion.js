
'use strict';
const $=id=>document.getElementById(id);
const tokenKey='waai_access_token';
const avatarKey='waai_avatar_data';
const motionKey='waai_pet_motion';
const visibleKey='waai_pet_visible';
let filter='all';
let petState='idle';
let idleReset=null;
let installPrompt=null;
let dragState=null;
let suppressPetClick=false;

async function api(path,opts={}){
 const headers={'Content-Type':'application/json',...(opts.headers||{})};
 const token=localStorage.getItem(tokenKey);
 if(token)headers.Authorization='Bearer '+token;
 const response=await fetch(path,{...opts,headers});
 if(response.status===204)return null;
 const raw=await response.text();
 let data;
 try{data=raw?JSON.parse(raw):null}catch{data=raw}
 if(!response.ok)throw new Error(typeof data==='string'?data:(data?.detail||'Error HTTP '+response.status));
 return data;
}
function setStatus(id,message,type=''){
 const node=$(id);if(!node)return;
 node.textContent=message||'';node.classList.toggle('error',type==='error');node.classList.toggle('ok',type==='ok');
}
function textNode(tag,text,className){
 const node=document.createElement(tag);node.textContent=String(text??'');
 if(className)node.className=className;
 return node;
}
function addButton(label,className,fn){
 const b=textNode('button',label,className||'soft-btn');b.type='button';b.addEventListener('click',fn);return b;
}
function go(name){
 document.querySelectorAll('.view').forEach(v=>v.classList.toggle('active',v.id===name));
 document.querySelectorAll('[data-g]').forEach(b=>b.classList.toggle('active',b.dataset.g===name));
 if(name==='chat')loadChat();
 if(name==='tareas')loadTasks(filter);
 if(name==='memoria')loadMemories();
 if(name==='investiga')loadResearch();
 if(name==='avisos')loadNotifications();
 if(name==='ajustes')loadConfig();
 if(name==='chat')$('petDock').classList.add('hidden');else applyPetVisibility();
 window.scrollTo({top:0,behavior:'smooth'});
}
document.querySelectorAll('[data-g]').forEach(b=>b.addEventListener('click',()=>go(b.dataset.g)));

function setPetState(state,message){
 petState=state;
 $('heroStage').dataset.state=state;
 $('petButton').dataset.state=state;
 const labels={idle:'En reposo',thinking:'Pensando',talking:'Conversando',research:'Investigando',happy:'¡Listo!',sleeping:'En pausa'};
 $('petStateText').textContent=labels[state]||labels.idle;
 $('stateDot').style.background=state==='research'? 'var(--orange)':state==='thinking'?'var(--purple)':state==='sleeping'?'var(--muted)':'var(--green)';
 if(message)$('petBubbleText').textContent=message;
 if(idleReset)clearTimeout(idleReset);
 if(['talking','happy','research','thinking'].includes(state))idleReset=setTimeout(()=>setPetState('idle'),state==='research'?6000:3200);
}
function applyAvatar(data){
 const source=data||'/static/assets/waai-companion.svg';
 ['brandAvatar','homeMascot','chatAvatar','settingsAvatar','floatingMascot'].forEach(id=>{const img=$(id);if(img)img.src=source});
 const backdrop=$('chatBackdrop');if(backdrop)backdrop.style.backgroundImage='linear-gradient(180deg,#17102028 0%,#17102070 35%,#100d17ef 100%),url("'+source+'")';
}
async function handleAvatarFile(file){
 if(!file)return;
 if(!file.type.startsWith('image/')){setStatus('avatarStatus','Elige un archivo de imagen.','error');return}
 if(file.size>12*1024*1024){setStatus('avatarStatus','La imagen supera 12 MB. Elige una versión de menos de 12 MB.','error');return}
 try{
  const bitmap=await createImageBitmap(file);
  const scale=Math.min(1,900/Math.max(bitmap.width,bitmap.height));
  const canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(bitmap.width*scale));canvas.height=Math.max(1,Math.round(bitmap.height*scale));
  const ctx=canvas.getContext('2d');ctx.drawImage(bitmap,0,0,canvas.width,canvas.height);bitmap.close();
  const data=canvas.toDataURL('image/jpeg',.82);
  localStorage.setItem(avatarKey,data);applyAvatar(data);setStatus('avatarStatus','Retrato guardado localmente.','ok');
 }catch(e){setStatus('avatarStatus','No pude procesar la imagen. Prueba con JPG o PNG.','error')}
}
function clearAvatar(){localStorage.removeItem(avatarKey);applyAvatar(null);$('avatarFile').value='';setStatus('avatarStatus','Mascota original restaurada.','ok')}
function applyPetVisibility(){
 const visible=localStorage.getItem(visibleKey)!=='false';
 $('petVisible').checked=visible;
 $('petDock').classList.toggle('hidden',!visible||$('chat').classList.contains('active'));
}
$('avatarFile').addEventListener('change',e=>handleAvatarFile(e.target.files&&e.target.files[0]));
$('petVisible').addEventListener('change',e=>{localStorage.setItem(visibleKey,String(e.target.checked));applyPetVisibility()});
$('petMotion').addEventListener('change',e=>{localStorage.setItem(motionKey,e.target.value);document.documentElement.dataset.motion=e.target.value});
const savedMotion=localStorage.getItem(motionKey)||'normal';$('petMotion').value=savedMotion;document.documentElement.dataset.motion=savedMotion;
const reducedMotion=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
if(reducedMotion)document.documentElement.dataset.motion='reduced';
applyAvatar(localStorage.getItem(avatarKey));applyPetVisibility();

function togglePetBubble(){
 const bubble=$('petBubble');bubble.hidden=!bubble.hidden;
 if(!bubble.hidden){
  const lines=['Puedo ayudarte a ordenar una idea, revisar pendientes o preparar una investigación.','¿Quieres que revisemos una prioridad pequeña y realista?','Tengo herramientas para tareas, memoria e investigación. ¿Por dónde empezamos?'];
  if(!bubble.dataset.opened){$('petBubbleText').textContent=lines[Math.floor(Math.random()*lines.length)];bubble.dataset.opened='1'}
 }
}
$('petButton').addEventListener('click',()=>{if(suppressPetClick){suppressPetClick=false;return}togglePetBubble()});

// Draggable companion dock. A tap opens the bubble; a drag only repositions it.
$('petButton').addEventListener('pointerdown',e=>{
 dragState={x:e.clientX,y:e.clientY,moved:false,startLeft:$('petDock').getBoundingClientRect().left,startTop:$('petDock').getBoundingClientRect().top};
 if($('petButton').setPointerCapture)$('petButton').setPointerCapture(e.pointerId);
});
$('petButton').addEventListener('pointermove',e=>{
 if(!dragState)return;
 const dx=e.clientX-dragState.x,dy=e.clientY-dragState.y;
 if(!dragState.moved&&Math.hypot(dx,dy)>9)dragState.moved=true;
 if(!dragState.moved)return;
 const dock=$('petDock');dock.style.right='auto';dock.style.left=Math.max(0,Math.min(window.innerWidth-dock.offsetWidth,dragState.startLeft+dx))+'px';
 dock.style.bottom='auto';dock.style.top=Math.max(60,Math.min(window.innerHeight-dock.offsetHeight-66,dragState.startTop+dy))+'px';
});
$('petButton').addEventListener('pointerup',()=>{if(dragState&&dragState.moved)suppressPetClick=true;dragState=null});
$('petButton').addEventListener('pointercancel',()=>{dragState=null});

function openPetChat(){$('petBubble').hidden=true;go('chat');$('chatText').focus()}
function petSuggest(){
 const pending=Number($('tasksN').textContent)||0;
 $('petBubble').hidden=false;
 $('petBubbleText').textContent=pending?'Tienes '+pending+' tarea(s) pendiente(s). ¿Quieres que elijamos una prioridad?':'Podríamos empezar con una idea, una tarea o una investigación corta.';
}
function startPriorityChat(){go('chat');send('Ayúdame a escoger una prioridad realista para hoy usando mis tareas pendientes.')}
function bubble(role,content){
 const n=textNode('div',content,'bubble '+role);$('chatlog').append(n);return n;
}
async function loadChat(){
 try{
  const messages=await api('/api/chat/history');$('chatlog').replaceChildren();
  if(!messages.length)bubble('assistant','Hola. Soy Waai. Podemos conversar, organizar tus tareas o preparar una investigación. ¿Qué tienes en mente?');
  messages.forEach(m=>bubble(m.role==='user'?'user':'assistant',m.content));
  $('chatlog').scrollTop=$('chatlog').scrollHeight;
 }catch(e){setStatus('chatStatus',e.message,'error')}
}
async function send(message){
 const text=String(message||'').trim();if(!text)return;
 go('chat');
 bubble('user',text);
 $('chatText').value='';
 $('typingIndicator').classList.add('active');
 $('chatStatus').textContent='Waai está pensando…';
 $('chatForm').querySelector('button').disabled=true;
 setPetState('thinking','Estoy leyendo tu mensaje…');
 try{
  const result=await api('/api/chat',{method:'POST',body:JSON.stringify({message:text})});
  bubble('assistant',result.reply||'He procesado tu mensaje.');
  if(result.sources?.length){
   result.sources.slice(0,6).forEach(source=>{
    const block=document.createElement('div');block.className='bubble assistant';
    block.append(textNode('strong',source.title));
    const url=textNode('a',source.url,'source-link');url.href=source.url;url.target='_blank';url.rel='noopener noreferrer';block.append(url);
    if(source.snippet)block.append(textNode('p',source.snippet,'meta'));
    $('chatlog').append(block);
   });
  }
  $('chatMode').textContent=result.llm_used?'Modelo lingüístico conectado':'Modo básico o acción estructurada';
  setStatus('chatStatus',result.intent==='research_assigned'?'Investigación registrada. Revisa fuentes en la sección Investigar.':'Respuesta recibida.','ok');
  setPetState(result.intent==='research_assigned'?'research':'talking',result.intent==='research_assigned'?'He empezado a revisar las fuentes para tu encargo.':'Ya estoy aquí. ¿Seguimos?');
  $('chatlog').scrollTop=$('chatlog').scrollHeight;
  await loadHome();
 }catch(e){
  setStatus('chatStatus',e.message,'error');
  setPetState('idle','No pude llegar al servidor. Comprueba la conexión de Waai.');
 }finally{
  $('typingIndicator').classList.remove('active');$('chatForm').querySelector('button').disabled=false;$('chatText').focus();
 }
}
$('chatForm').addEventListener('submit',e=>{e.preventDefault();send($('chatText').value)});
async function clearChat(){
 if(!confirm('¿Borrar todo el historial de conversación de Waai?'))return;
 try{await api('/api/chat/history',{method:'DELETE'});await loadChat();setStatus('chatStatus','Historial borrado.','ok')}catch(e){setStatus('chatStatus',e.message,'error')}
}

function makeTaskItem(task,compact=false){
 const item=document.createElement('div');item.className='item';
 const heading=document.createElement('div');heading.className='between';
 const left=document.createElement('div');left.className='grow';
 left.append(textNode('div',task.title,'item-title'));
 const due=task.due_at?' · '+new Date(task.due_at).toLocaleString('es-CO',{dateStyle:'short',timeStyle:'short'}):'';
 left.append(textNode('div',String(task.priority||'normal')+' · '+String(task.status||'pending')+due,'meta'));
 heading.append(left);
 if(task.status==='pending')heading.append(addButton('✓','primary-btn tiny-btn',()=>completeTask(task.id)));
 item.append(heading);
 if(task.details)item.append(textNode('p',task.details,'muted tiny'));
 if(!compact)item.append(addButton('Eliminar','danger-btn tiny-btn',()=>deleteTask(task.id)));
 return item;
}
async function loadTasks(status='all'){
 filter=status||filter;
 try{
  const tasks=await api('/api/tasks?status='+encodeURIComponent(filter));const root=$('taskList');root.replaceChildren();
  if(!tasks.length){root.append(textNode('div',filter==='completed'?'Todavía no hay tareas completadas.':'No hay tareas en esta lista.','empty'));return}
  tasks.forEach(t=>root.append(makeTaskItem(t)));
 }catch(e){setStatus('taskStatus',e.message,'error')}
}
async function completeTask(id){
 try{await api('/api/tasks/'+id,{method:'PATCH',body:JSON.stringify({status:'completed'})});setPetState('happy','¡Una tarea menos! Buen trabajo.');await loadTasks(filter);await loadHome()}catch(e){alert(e.message)}
}
async function deleteTask(id){
 if(!confirm('¿Eliminar esta tarea?'))return;
 try{await api('/api/tasks/'+id,{method:'DELETE'});await loadTasks(filter);await loadHome()}catch(e){alert(e.message)}
}
$('taskForm').addEventListener('submit',async e=>{
 e.preventDefault();
 try{
  const due=$('taskDue').value?new Date($('taskDue').value).toISOString():null;
  await api('/api/tasks',{method:'POST',body:JSON.stringify({title:$('taskTitle').value,details:$('taskDetails').value,priority:$('taskPriority').value,due_at:due})});
  e.target.reset();setStatus('taskStatus','Tarea guardada.','ok');setPetState('happy','He anotado esa tarea para ti.');await loadTasks('all');await loadHome();
 }catch(error){setStatus('taskStatus',error.message,'error')}
});

async function loadMemories(){
 try{
  const memories=await api('/api/memories'),root=$('memoryList');root.replaceChildren();
  if(!memories.length){root.append(textNode('div','No hay recuerdos guardados.','empty'));return}
  memories.forEach(memory=>{
   const item=document.createElement('div');item.className='item';const heading=document.createElement('div');heading.className='between';
   heading.append(textNode('strong',memory.memory_key,'item-title'));
   heading.append(addButton('Borrar','danger-btn tiny-btn',()=>deleteMemory(memory.id)));
   item.append(heading,textNode('p',memory.memory_value,'muted'),textNode('div',memory.category+' · '+memory.created_at,'meta'));root.append(item);
  });
 }catch(e){setStatus('memoryStatus',e.message,'error')}
}
$('memoryForm').addEventListener('submit',async e=>{
 e.preventDefault();
 try{
  await api('/api/memories',{method:'POST',body:JSON.stringify({key:$('memKey').value,value:$('memValue').value,category:'user_approved',confirmed:$('memConfirm').checked})});
  e.target.reset();setStatus('memoryStatus','Recuerdo guardado con tu autorización.','ok');await loadMemories();await loadHome();
 }catch(error){setStatus('memoryStatus',error.message,'error')}
});
async function deleteMemory(id){
 if(!confirm('¿Borrar este recuerdo?'))return;
 try{await api('/api/memories/'+id,{method:'DELETE'});await loadMemories();await loadHome()}catch(e){alert(e.message)}
}
async function deleteAllMemories(){
 if(!confirm('¿Borrar todos los recuerdos guardados?'))return;
 try{await api('/api/memories',{method:'DELETE'});await loadMemories();await loadHome()}catch(e){alert(e.message)}
}

async function loadResearch(){
 try{
  const items=await api('/api/research'),root=$('researchList');root.replaceChildren();
  if(!items.length){root.append(textNode('div','Asigna un tema para comenzar una investigación.','empty'));return}
  items.forEach(research=>{
   const item=document.createElement('div');item.className='item';
   const head=document.createElement('div');head.className='between';head.append(textNode('strong',research.topic,'item-title'),textNode('span',research.status,'meta'));item.append(head);
   if(research.scope)item.append(textNode('p',research.scope,'muted tiny'));
   (research.sources||[]).forEach(source=>{
    const sourceBox=document.createElement('div');sourceBox.className='item';
    const link=textNode('a',source.title||source.url);link.href=source.url;link.target='_blank';link.rel='noopener noreferrer';sourceBox.append(link);
    if(source.snippet)sourceBox.append(textNode('p',source.snippet,'muted tiny'));
    sourceBox.append(textNode('div','Recopilado: '+(source.retrieved_at||'fecha no disponible'),'meta'));
    const actions=document.createElement('div');actions.className='row';actions.append(addButton('Aprobar resumen','primary-btn tiny-btn',()=>approveKnowledge(research.id,source.url,source.title||'',source.snippet||'')));sourceBox.append(actions);item.append(sourceBox);
   });
   if(research.status!=='saved')item.append(addButton('Reintentar búsqueda','soft-btn tiny-btn',()=>retryResearch(research.id)));
   root.append(item);
  });
 }catch(e){setStatus('researchStatus',e.message,'error')}
}
$('researchForm').addEventListener('submit',async e=>{
 e.preventDefault();
 try{
  setPetState('research','He recibido una nueva investigación.');
  const domains=$('researchDomains').value.split(',').map(x=>x.trim()).filter(Boolean);
  await api('/api/research',{method:'POST',body:JSON.stringify({topic:$('researchTopic').value,scope:$('researchScope').value,allowed_domains:domains})});
  e.target.reset();setStatus('researchStatus','Investigación registrada. Revisa las fuentes y el resumen antes de aprobar conocimiento.','ok');
  await loadResearch();await loadHome();setPetState('talking','La investigación está lista para que revises lo encontrado.');
 }catch(error){setStatus('researchStatus',error.message,'error');setPetState('idle')}
});
async function retryResearch(id){
 try{setPetState('research','Estoy volviendo a buscar…');await api('/api/research/'+id+'/run',{method:'POST'});await loadResearch();await loadHome();setPetState('talking','Ya actualicé los resultados disponibles.')}catch(e){alert(e.message);setPetState('idle')}
}
async function approveKnowledge(id,url,title,snippet){
 const summary=prompt('Revisa y edita el resumen que quieres conservar:',(title?title+'\n\n':'')+snippet);
 if(summary===null||!summary.trim())return;
 if(!confirm('¿Has revisado la fuente y autorizas guardar este resumen en la memoria de conocimiento?'))return;
 try{await api('/api/research/'+id+'/save',{method:'POST',body:JSON.stringify({summary:summary,source_url:url,approved:true})});alert('Conocimiento guardado con tu aprobación.');await loadResearch();await loadHome()}catch(e){alert(e.message)}
}

async function loadNotifications(){
 try{
  const notes=await api('/api/notifications'),root=$('notificationList');root.replaceChildren();
  if(!notes.length){root.append(textNode('div','Sin avisos por ahora.','empty'));return}
  notes.forEach(note=>{
   const item=document.createElement('div');item.className='item';const head=document.createElement('div');head.className='between';head.append(textNode('strong',note.title,'item-title'));
   if(note.read_at)head.append(textNode('span','Leído','meta'));else head.append(addButton('Marcar leído','soft-btn tiny-btn',()=>readNotice(note.id)));
   item.append(head,textNode('p',note.body,'muted'),textNode('div',note.created_at,'meta'));root.append(item);
  });
 }catch(e){$('notificationList').textContent=e.message}
}
async function readNotice(id){try{await api('/api/notifications/'+id+'/read',{method:'POST'});await loadNotifications();await loadHome()}catch(e){alert(e.message)}}
async function markAllRead(){try{await api('/api/notifications/read-all',{method:'POST'});await loadNotifications();await loadHome()}catch(e){alert(e.message)}}

async function loadHome(){
 try{
  const overview=await api('/api/overview');
  $('connection').textContent='Conectada · '+new Date(overview.server_time).toLocaleTimeString('es-CO',{hour:'2-digit',minute:'2-digit'});
  $('connectionDot').style.background='var(--green)';
  $('tasksN').textContent=overview.tasks.length;$('memoryN').textContent=overview.memory_count;$('researchN').textContent=overview.research_pending;$('noticeN').textContent=overview.unread_notifications;
  $('proactive').textContent=overview.proactive_message;
  const homeTasks=$('homeTasks');homeTasks.replaceChildren();
  if(!overview.tasks.length)homeTasks.append(textNode('div','No hay tareas pendientes guardadas. ¿Quieres añadir una?','empty'));
  else overview.tasks.slice(0,4).forEach(t=>homeTasks.append(makeTaskItem(t,true)));
  const feed=$('homeFeed');feed.replaceChildren();
  if(overview.unread_notifications>0)feed.append(textNode('div','Tienes '+overview.unread_notifications+' aviso(s) sin leer.','item-title'));
  if(overview.research_pending>0)feed.append(textNode('p',overview.research_pending+' investigación(es) abiertas o pendientes de revisar.','muted'));
  if(!overview.unread_notifications&&!overview.research_pending)feed.append(textNode('p','Sin alertas urgentes. Puedo ayudarte a planear una prioridad o investigar un tema cuando lo necesites.','muted'));
  const config=await api('/api/config');
  $('initiativeNote').textContent=config.autonomy_mode==='research'&&config.autonomous_research_interests>0?'Iniciativa activada: puedo investigar un tema de tu lista diaria mientras el servidor esté funcionando. Los resultados quedan como borradores, no como memoria automática.':'La iniciativa automática está desactivada o no tiene temas autorizados. Puedes configurarla desde el servidor siguiendo la guía de Ajustes.';
  if(config.llm_connected){$('chatMode').textContent='Modelo conectado · '+(config.llm_model||'');$('modelName').textContent=config.llm_model||'Modelo conectado';$('modelDescription').textContent='Conversación generativa disponible.'}
  else{$('modelName').textContent='Modo de reglas';$('modelDescription').textContent='No hay un modelo lingüístico configurado; el motor de conversación es limitado.'}
  $('researchMode').textContent=config.research_provider_connected?'Activada':'Desactivada';
  $('autonomyMode').textContent=config.autonomy_mode==='research'&&config.autonomous_research_interests>0?'Activa':'Desactivada';
  $('timezoneMode').textContent=config.timezone||'America/Bogota';
  $('autonomyInfo') && ($('autonomyInfo').textContent=config.autonomy_mode||'off');
 }catch(e){
  $('connection').textContent='Sin conexión o falta token';$('connectionDot').style.background='var(--red)';$('proactive').textContent=e.message;
  $('homeFeed').replaceChildren(textNode('div','No pude leer el resumen. Revisa que el servidor de Waai siga activo.','empty'));
 }
}
async function loadConfig(){
 try{
  const config=await api('/api/config');$('token').value=localStorage.getItem(tokenKey)||'';
  $('modelName').textContent=config.llm_connected?(config.llm_model||'Modelo conectado'):'Modo de reglas';
  $('modelDescription').textContent=config.llm_connected?'La conversación generativa está disponible a través del proveedor configurado.':'No se detectó un modelo lingüístico. Las respuestas siguen usando reglas básicas.';
  $('researchMode').textContent=config.research_provider_connected?'Activada':'Desactivada';
  $('autonomyMode').textContent=config.autonomy_mode==='research'&&config.autonomous_research_interests>0?'Activa · '+config.autonomous_research_interests+' temas':'Desactivada';
  $('timezoneMode').textContent=config.timezone||'America/Bogota';
 }catch(e){setStatus('tokenStatus',e.message,'error')}
}
function saveToken(){localStorage.setItem(tokenKey,$('token').value.trim());setStatus('tokenStatus','Token guardado localmente.','ok');loadHome();loadConfig()}
function clearToken(){localStorage.removeItem(tokenKey);$('token').value='';setStatus('tokenStatus','Token eliminado de este navegador.','ok');loadHome()}
function petSuggest(){const pending=Number($('tasksN').textContent)||0;const research=Number($('researchN').textContent)||0;$('petBubble').hidden=false;$('petBubbleText').textContent=pending?'Veo '+pending+' pendiente(s). Podemos escoger una acción pequeña para empezar.':research?'Hay '+research+' investigación(es) por revisar. ¿Miramos una?':'¿Qué te parece si empezamos por una idea o una pregunta que tengas rondando?'}
function openPetChat(){ $('petBubble').hidden=true;go('chat');$('chatText').focus() }

window.addEventListener('beforeinstallprompt',event=>{event.preventDefault();installPrompt=event;$('installBtn').classList.remove('hide')});
$('installBtn').addEventListener('click',async()=>{if(!installPrompt)return;installPrompt.prompt();await installPrompt.userChoice;installPrompt=null;$('installBtn').classList.add('hide')});
if('serviceWorker'in navigator)window.addEventListener('load',()=>navigator.serviceWorker.register('/sw.js').catch(()=>{}));
async function boot(){
 await loadHome();
 try{await loadTasks('all');await loadMemories();await loadResearch();await loadNotifications();await loadConfig()}catch{}
}
boot();
window.addEventListener('online',()=>loadHome());
document.addEventListener('visibilitychange',()=>{if(!document.hidden)loadHome()});
