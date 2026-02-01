import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { encode } from 'plantuml-encoder';

@Component({
  selector: 'app-plantuml-viewer',
  imports: [],
  templateUrl: './plantuml-viewer.html',
  styleUrl: './plantuml-viewer.scss',
})
export class PlantumlViewer implements OnInit {
  projectName = '';
  pumlContent = '';
  diagramUrl = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.projectName = params['projectName'] || '';
      this.pumlContent = params['pumlContent'] || '';
      
      if (this.pumlContent) {
        const encoded = encode(this.pumlContent);
        this.diagramUrl = `https://www.plantuml.com/plantuml/svg/${encoded}`;
      }
    });
  }

  addNewRequirement(): void {
    this.router.navigate(['/chat'], { 
      queryParams: { projectName: this.projectName } 
    });
  }
}
