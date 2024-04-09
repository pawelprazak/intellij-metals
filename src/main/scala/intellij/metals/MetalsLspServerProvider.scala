package intellij.metals

import com.intellij.codeInsight.completion.{CompletionParameters, CompletionType}
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider.LspServerStarter
import com.intellij.platform.lsp.api.customization.LspCompletionSupport
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem
import com.intellij.platform.lsp.api.{LspServer, LspServerSupportProvider, ProjectWideLspServerDescriptor}
import intellij.metals.settings.{MetalsConfigurable, MetalsSettings}
import intellij.metals.ui.Icons

final class MetalsLspServerProvider extends LspServerSupportProvider {
  override def fileOpened(
    project: Project,
    file: VirtualFile,
    serverStarter: LspServerStarter
  ): Unit = {
    val settings = MetalsSettings.getInstance
    if (settings.metalsPath.nonEmpty) {
      serverStarter.ensureServerStarted(MetalsLspServerDescriptor(project, settings.metalsPath))
    }
  }

  override def createLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile): LspServerWidgetItem =
    new LspServerWidgetItem(lspServer, currentFile, Icons.Metals, classOf[MetalsConfigurable])

  private case class MetalsLspServerDescriptor(project: Project, metalsPath: String)
      extends ProjectWideLspServerDescriptor(project, "Metals") {

    override def createCommandLine: GeneralCommandLine =
      new GeneralCommandLine(metalsPath) // TODO make sure the path actually exists

    override def isSupportedFile(file: VirtualFile): Boolean =
      file.getExtension == "scala"

    // Disable the built-in hover support to disable the hover info tooltip
    // Macro type evaluator will create an ad-hoc hover request to the LSP server
    override def getLspHoverSupport: Boolean = false

    override def getLspCompletionSupport: LspCompletionSupport = new LspCompletionSupport {
      override def shouldRunCodeCompletion(parameters: CompletionParameters): Boolean =
        parameters.getCompletionType == CompletionType.SMART
    }
  }
}
