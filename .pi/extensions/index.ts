import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";
import teamOrchestrator from "./team-orchestrator.js";
import { registerSecurityGuard } from "./security-guard.js";

export default function (pi: ExtensionAPI) {
  registerSecurityGuard(pi);
  teamOrchestrator(pi);
}
