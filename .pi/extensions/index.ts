import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";
import teamOrchestrator from "./team-orchestrator.js";

export default function (pi: ExtensionAPI) {
  teamOrchestrator(pi);
}
