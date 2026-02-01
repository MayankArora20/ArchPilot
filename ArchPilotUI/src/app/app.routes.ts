import { Routes } from '@angular/router';
import { Landing } from './components/landing/landing';
import { AddProject } from './components/add-project/add-project';
import { ExistingProject } from './components/existing-project/existing-project';
import { PlantumlViewer } from './components/plantuml-viewer/plantuml-viewer';
import { Chat } from './components/chat/chat';
import { About } from './components/about/about';

export const routes: Routes = [
  { path: '', component: Landing },
  { path: 'add-project', component: AddProject },
  { path: 'existing-project', component: ExistingProject },
  { path: 'plantuml-viewer', component: PlantumlViewer },
  { path: 'chat', component: Chat },
  { path: 'about', component: About },
  { path: '**', redirectTo: '' }
];
